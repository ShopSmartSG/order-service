package sg.edu.nus.iss.order_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.core.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.order_service.db.MongoManager;
import sg.edu.nus.iss.order_service.model.*;
import sg.edu.nus.iss.order_service.utils.Constants;
import sg.edu.nus.iss.order_service.utils.Utils;
import sg.edu.nus.iss.order_service.utils.WSUtils;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService extends Constants {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final ObjectMapper mapper = Json.mapper();

    private final CartService cartService;
    private final MongoManager mongoManager;
    private final WSUtils wsUtils;
    private final Utils utils;

    @Value("${"+ORDER_DB+"}")
    private String orderDb;

    @Value("${"+ORDER_COLLECTION+"}")
    private String orderColl;

    @Value("${"+COMPLETED_ORDERS_COLL+"}")
    private String completedOrderColl;

    @Value("${"+CANCELLED_ORDERS_COLL+"}")
    private String cancelledOrderColl;

    @Value("${product.service.url}")
    private String productServiceUrl = "http://product-service:95/"; //http://product-service:95/

    @Value("${profile.service.url}")
    private String profileServiceUrl = "http://profile-service:80/"; //http://profile-service:80/

    @Value("${delivery.service.url}")
    private String deliveryServiceUrl = "http://delivery-service:92/"; //http://delivery-service:92/

    @Autowired
    public OrderService(CartService cartService, MongoManager mongoManager, WSUtils wsUtils, Utils utils) {
        this.cartService = cartService;
        this.mongoManager = mongoManager;
        this.wsUtils = wsUtils;
        this.utils = utils;
    }

    public Response createOrderFromCart(String customerId, boolean useRewards, boolean useDelivery){
        log.info("Creating order for customer: {}", customerId);
        Cart cart = cartService.getCartByCustomerId(customerId);
        if(cart == null || cart.getCartItems().isEmpty()){
            log.error("No items found in cart for customer: {}, so no order to be created", customerId);
            return utils.getFailedResponse("No items found in cart for customer: ".concat(customerId).concat(", so no order to be created"));
        }
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setCustomerId(customerId);
        order.setMerchantId(cart.getMerchantId());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedDate(System.currentTimeMillis());
        order.setUpdatedDate(System.currentTimeMillis());
        order.setCreatedBy(CUSTOMER);
        order.setUpdatedBy(CUSTOMER);
        order.setUseRewards(useRewards);
        order.setUseDelivery(useDelivery);

        List<Product> productDetails = getProductDetailsForItems(cart.getCartItems());
        if(productDetails == null || productDetails.isEmpty()){
            log.error("Failed to get product details or found non matching products for items in cart for customer: {}", customerId);
            return utils.getFailedResponse("Failed to get product details or found non matching products for items in cart for customer: ".concat(customerId));
        }

        List<Item> cartItemsWithPrice = updatedCartItemsListBasedOnProductDetails(productDetails, cart.getCartItems());
        order.setOrderItems(cartItemsWithPrice);
        order.setTotalPrice(calculateTotalPrice(cartItemsWithPrice));

        //fetch customer reward points to offset
        if(order.isUseRewards()){
            JsonNode rewrdsObject = getRewardPointsOffsetForCustomer(order.getCustomerId());
            if(rewrdsObject!=null){
                BigDecimal rewardPointsAmountOffset = rewrdsObject.get("rewardAmount").decimalValue();
                BigDecimal rewardsPoints = rewrdsObject.get("rewardPoints").decimalValue();
                order.setRewardsAmountUsed(rewardPointsAmountOffset);
                order.setCustomerRewardsPointsUsed(rewardsPoints);
                order.setTotalPrice(order.getTotalPrice().subtract(rewardPointsAmountOffset));
                updateCustomerRewardPoints(order.getOrderId(), order.getCustomerId(), BigDecimal.ZERO);
            } else {
                order.setRewardsAmountUsed(BigDecimal.ZERO);
                order.setCustomerRewardsPointsUsed(BigDecimal.ZERO);
            }
        } else {
            order.setRewardsAmountUsed(BigDecimal.ZERO);
            order.setCustomerRewardsPointsUsed(BigDecimal.ZERO);
        }

        log.info("Order to be created : {}", order);

        Document insertDocument = mapper.convertValue(order, Document.class);
        boolean result = mongoManager.insertDocument(insertDocument, orderDb, orderColl);
        if(result){
            log.info("Order created successfully for customer: {}", customerId);
            cartService.deleteCartByCustomerId(customerId);
            List<ProductUpdateReqModel> productsToBeUpdated = generateProductUpdateReqObts(productDetails, cart.getCartItems());
            //url will be merchants/{merchantId}/products/{productId}
            String url = productServiceUrl.concat("merchants");
            int errorCounts = 0;
            for(ProductUpdateReqModel reqProd : productsToBeUpdated){
                log.info("Updating stock for productId: {}", reqProd.getProductId());
                String reqUrl = url.concat(SLASH).concat(reqProd.getMerchantId().toString()).concat(SLASH)
                        .concat("products").concat(SLASH).concat(reqProd.getProductId().toString());
                JsonNode payload = mapper.convertValue(reqProd, JsonNode.class);
                try{
                    Response response = wsUtils.makeWSCallObject(reqUrl, payload, new HashMap<>(), HttpMethod.PUT, 1000, 30000);
                    if(SUCCESS.equalsIgnoreCase(response.getStatus())){
                        log.info("Product stock updated successfully for productId: {}", reqProd.getProductId());
                    }else{
                        log.error("Failed to update product stock for productId: {}", reqProd.getProductId());
                        errorCounts++;
                    }
                }catch(Exception ex){
                    log.error("Exception occurred while updating stock for productId: {}", reqProd.getProductId());
                    errorCounts++;
                }
            }
            if(errorCounts>0){
                //TODO :: have to revert the counts of stock which were reduced.
                //TODO :: also explore if you can restore cart as well.
                log.error("Failed to update stock for {} products out of {}, so deleting previously created order",
                        errorCounts, productsToBeUpdated.size());
                deleteOrder(order.getOrderId());
                return utils.getFailedResponse("Failed to create order for customer: ".concat(customerId));
            }
            return utils.getSuccessResponse("Order created successfully for customer: ".concat(customerId).concat(" with orderId: ").concat(order.getOrderId()), null);
        }else{
            log.error("Failed to create order for customer: {}", customerId);
            return utils.getFailedResponse("Failed to create order for customer: ".concat(customerId));
        }
    }

    private List<Product> getProductDetailsForItems(List<Item> cartItems){
        String prodIds = "";
        for(Item item : cartItems){
            prodIds = prodIds.concat(item.getProductId()).concat(",");
        }
        prodIds = prodIds.substring(0, prodIds.length()-1);
        //url will be products/ids with query param as "productIds"
        String productIdsListUrl = productServiceUrl.concat("products/ids");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(productIdsListUrl);
        uriBuilder.queryParam("productIds", prodIds);
        log.debug("URL to get product details for productIds is : {}", uriBuilder.toUriString());
        try{
            Response response = wsUtils.makeWSCallObject(uriBuilder.toUriString(), null, new HashMap<>(), HttpMethod.GET, 1000, 30000);
            if(FAILURE.equalsIgnoreCase(response.getStatus())){
                log.error("Failed to get product details for productIds: {}", prodIds);
                return new ArrayList<>();
            }
            ArrayNode data = (ArrayNode) response.getData();
            List<Product> products = mapper.convertValue(data, new TypeReference<List<Product>>() {});
            if(products == null || products.isEmpty()){
                return new ArrayList<>();
            }
            log.info("Product details found for productIds: {}", prodIds);
            return products;
        }catch(Exception ex){
            log.error("Exception occurred while getting product details for productIds: {}", prodIds);
            return new ArrayList<>();
        }
    }

    private List<Item> updatedCartItemsListBasedOnProductDetails(List<Product> productDetails, List<Item> cartItems){
        List<Item> itemsForOrder = new ArrayList<>();
        for (Product productDetail : productDetails) {
            for (Item cartItem : cartItems) {
                log.debug("in this iteration checking itemNode : {} and item : {}", productDetail, cartItem);
                if (cartItem.getProductId().equalsIgnoreCase(productDetail.getProductId().toString())) {
                    cartItem.setPrice(productDetail.getListingPrice());
                    itemsForOrder.add(cartItem);
                }
            }
        }
        return itemsForOrder;
    }

    private List<ProductUpdateReqModel> generateProductUpdateReqObts(List<Product> products, List<Item> cartItems){
        log.info("Starting generateProductUpdateReqObts, products: {}, cartItems: {}", products, cartItems);
        Map<UUID, ProductUpdateReqModel> productUpdateReqMap = new HashMap<>();
        for(Product prod : products){
            ProductUpdateReqModel reqModel = mapper.convertValue(prod, ProductUpdateReqModel.class);
            reqModel.setCategoryId(prod.getCategory().getCategoryId());
            productUpdateReqMap.put(prod.getProductId(), reqModel);
        }
        for(Item item : cartItems){
            ProductUpdateReqModel reqModel = productUpdateReqMap.get(UUID.fromString(item.getProductId()));
            reqModel.setAvailableStock(reqModel.getAvailableStock() - item.getQuantity());
            productUpdateReqMap.put(UUID.fromString(item.getProductId()), reqModel);
        }
        log.debug("Final productUpdateReqMap : {}", productUpdateReqMap);
        return  new ArrayList<>(productUpdateReqMap.values());
    }

    private JsonNode getRewardPointsOffsetForCustomer(String customerId){
        //from profile service needs to hit path : customers/{customer-id}/rewards  a GET call
        String url = profileServiceUrl.concat("customers").concat(SLASH).concat(customerId).concat(SLASH).concat("rewards");
        log.debug("URL to get reward points and offset amount for customerId: {} is {}", customerId, url);
        try{
            Response response = wsUtils.makeWSCallObject(url, null, new HashMap<>(), HttpMethod.GET, 1000, 30000);
            if(FAILURE.equalsIgnoreCase(response.getStatus())){
                log.error("Failed to get reward points and offset amount for customerId: {}", customerId);
            }
            JsonNode data = response.getData();
            return data;
        }catch(Exception ex){
            log.error("Exception occurred while getting reward points and offset amount for customerId: {}", customerId);
            return null;
        }
    }

    private BigDecimal calculateTotalPrice(List<Item> cartItems) {
        return cartItems.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Response getOrdersListByProfileId(String listType, String profileType, String id){
        log.info("Fetching orders for profileType: {}, id: {}", profileType, id);
        String profileIdentifierField = utils.getProfileIdentifierFieldBasedOnRole(profileType);
        if(StringUtils.isEmpty(profileIdentifierField)){
            log.error("Invalid profileType provided for fetching orders, profileType: {}", profileType);
            return utils.getFailedResponse("Invalid profileType provided for fetching orders");
        }
        if(COMPLETED.equalsIgnoreCase(listType)){
            log.info("Fetching completed orders for profileType: {}, id: {}", profileType, id);
            return getCompletedOrdersByProfileId(id, profileIdentifierField);
        }else if(CANCELLED.equalsIgnoreCase(listType)){
            log.info("Fetching cancelled orders for profileType: {}, id: {}", profileType, id);
            return getCancelledOrdersByProfileId(id, profileIdentifierField);
        }else if(ACTIVE.equalsIgnoreCase(listType)){
            log.info("Fetching active orders for profileType: {}, id: {}", profileType, id);
            return getActiveOrdersByProfileId(id, profileIdentifierField);
        }else{
            log.info("Fetching all orders for profileType: {}, id: {}", profileType, id);
            return getAllOrdersByProfileId(id, profileIdentifierField);
        }
    }

    private Response getCompletedOrdersByProfileId(String profileId, String profileIdKey){
        log.info("Fetching completed orders for profileId: {}", profileId);
        Document query = new Document(profileIdKey, profileId);
        log.info("Query to fetch completed orders : {} from completedOrders coll", query);
        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, completedOrderColl);
        if(orders!=null && !orders.isEmpty()){
            log.info("Found completed orders for profileKey {}, profileId: {}, count {}", profileIdKey, profileId, orders.size());
            List<Order> orderList = mapper.convertValue(orders, List.class);
            return utils.getSuccessResponse("Completed orders found for ".concat(profileIdKey)
                    .concat(" :: ").concat(profileId),mapper.convertValue(orderList, ArrayNode.class));
        }else{
            log.info("No completed orders found for profileKey {}, profileId: {}", profileIdKey, profileId);
            return utils.getFailedResponse("No completed orders found for profileKey ".concat(profileIdKey)
                    .concat(", profileId: ").concat(profileId));
        }
    }

    private Response getCancelledOrdersByProfileId(String profileId, String profileIdKey){
        log.info("Fetching cancelled orders for profileId: {}", profileId);
        Document query = new Document(profileIdKey, profileId);
        log.info("Query to fetch cancelled orders : {} from cancelledOrders coll", query);
        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, cancelledOrderColl);
        if(orders!=null && !orders.isEmpty()){
            log.info("Found cancelled orders for profileKey {}, profileId: {}, count {}", profileIdKey, profileId, orders.size());
            List<Order> orderList = mapper.convertValue(orders, List.class);
            return utils.getSuccessResponse("Cancelled orders found for ".concat(profileIdKey)
                    .concat(" :: ").concat(profileId),mapper.convertValue(orderList, ArrayNode.class));
        }else{
            log.info("No cancelled orders found for profileKey {}, profileId: {}", profileIdKey, profileId);
            return utils.getFailedResponse("No cancelled orders found for profileKey ".concat(profileIdKey)
                    .concat(", profileId: ").concat(profileId));
        }
    }

    private Response getActiveOrdersByProfileId(String profileId, String profileIdKey){
        log.info("Fetching active orders for profileId: {}", profileId);
        Document query = new Document(profileIdKey, profileId);
        log.info("Query to fetch only active orders : {} from orders coll", query);
        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, orderColl);
        if(orders!=null && !orders.isEmpty()){
            log.info("Found active orders for profileKey {}, profileId: {}, count {}", profileIdKey, profileId, orders.size());
            List<Order> orderList = mapper.convertValue(orders, List.class);
            return utils.getSuccessResponse("Active orders found for ".concat(profileIdKey)
                    .concat(" :: ").concat(profileId),mapper.convertValue(orderList, ArrayNode.class));
        }else{
            log.info("No active orders found for profileKey {}, profileId: {}", profileIdKey, profileId);
            return utils.getFailedResponse("No active orders found for profileKey ".concat(profileIdKey)
                    .concat(", profileId: ").concat(profileId));
        }
    }

    private Response getAllOrdersByProfileId(String profileId, String profileIdKey){
        log.info("Fetching orders for profileId: {}", profileId);
        Document query = new Document(profileIdKey, profileId);
        List<Document> totalOrders = new ArrayList<>();
        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, orderColl);
        List<Document> completedOrders = mongoManager.findAllDocuments(query, orderDb, completedOrderColl);
        List<Document> cancelledOrders = mongoManager.findAllDocuments(query, orderDb, cancelledOrderColl);
        if(orders!=null && !orders.isEmpty()){
            log.info("Found active orders for profileKey {}, profileId: {}, count {}", profileIdKey, profileId, orders.size());
            totalOrders.addAll(orders);
        }
        if(completedOrders!=null && !completedOrders.isEmpty()){
            log.info("Found completed orders for profileKey {}, profileId: {}, count {}", profileIdKey, profileId, completedOrders.size());
            totalOrders.addAll(completedOrders);
        }
        if(cancelledOrders!=null && !cancelledOrders.isEmpty()){
            log.info("Found cancelled orders for profileKey {}, profileId: {}, count {}", profileIdKey, profileId, cancelledOrders.size());
            totalOrders.addAll(cancelledOrders);
        }
        if(totalOrders!=null && !totalOrders.isEmpty()){
            log.info("Found total orders for profileKey {}, profileId: {}, as count {}", profileIdKey, profileId, totalOrders.size());
            List<Order> orderList = mapper.convertValue(totalOrders, List.class);
            return utils.getSuccessResponse("Total orders found for ".concat(profileIdKey)
                    .concat(" :: ").concat(profileId),mapper.convertValue(orderList, ArrayNode.class));
        }else{
            log.info("No orders found for profileKey {}, profileId: {}", profileIdKey, profileId);
            return utils.getFailedResponse("No orders found for profileKey ".concat(profileIdKey)
                    .concat(", profileId: ").concat(profileId));
        }
    }

    public Response getActiveOrdersForDelivery(){
        log.info("Fetching all active orders available for delivery, having status READY and opted for delivery");
        Document query = new Document(STATUS, OrderStatus.READY);
        query.put(USE_DELIVERY, true);
        log.info("Query to fetch active orders for delivery : {} from orders coll", query);
        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, orderColl);
        if(orders!=null && !orders.isEmpty()){
            log.info("Found active orders for delivery having status READY and opted for delivery, count {}", orders.size());
            List<Order> orderList = mapper.convertValue(orders, List.class);
            return utils.getSuccessResponse("Active orders found for delivery",mapper.convertValue(orderList, ArrayNode.class));
        }else{
            log.info("No active orders found for delivery having status READY and opted for delivery");
            return utils.getFailedResponse("No active orders found for delivery having status READY and opted for delivery");
        }
    }

    public Response getOrderByOrderId(String orderId){
        log.info("Fetching order by orderId: {}", orderId);
        Document query = new Document(ORDER_ID, orderId);
        Document resDoc = mongoManager.findDocument(query, orderDb, orderColl);
        if(resDoc!=null && !resDoc.isEmpty()){
            log.info("Found orders for provided orderId: {}", orderId);
            Order order = mapper.convertValue(resDoc, Order.class);
            return utils.getSuccessResponse("Order found for orderId: ".concat(orderId), mapper.convertValue(order, JsonNode.class));
        }else{
            log.info("No orders found for orderId: {}", orderId);
            return utils.getFailedResponse("No orders found for orderId: ".concat(orderId));
        }
    }

    //later on we need to implement Chain of Responsibility pattern to handle this.
    public Response updateOrderStatus(String orderId, OrderStatus status, JsonNode payload){
        log.info("Updating order status for orderId: {} to status : {}", orderId, status);
        Document query = new Document(ORDER_ID, orderId);
        Document orderDoc = mongoManager.findDocument(query, orderDb, orderColl);
        if(orderDoc == null){
            log.error("No order found for orderId: {}", orderId);
            return utils.getFailedResponse("No order found for orderId: ".concat(orderId));
        }
        if(status.equals(OrderStatus.ACCEPTED) || status.equals(OrderStatus.READY)){
            //update document in order coll only.
            Document updateDoc = new Document(STATUS, status);
            updateDoc.put(UPDATED_AT, System.currentTimeMillis());
            updateDoc.put(UPDATED_BY, MERCHANT);
            log.info("Updating order status for orderId: {} using query : {}", orderId, mapper.convertValue(query, JsonNode.class));
            Document result = mongoManager.findOneAndUpdate(query, new Document("$set", updateDoc), orderDb, orderColl, false, true);
            if(result != null){
                log.info("Order status updated successfully for orderId: {} to status : {}", orderId, status);
                return utils.getSuccessResponse("Order status updated successfully for orderId: ".concat(orderId).concat(" to status : ").concat(status.toString()), null);
            }else{
                log.error("Failed to update order status for orderId: {} to status : {}", orderId, status);
                return utils.getFailedResponse("Failed to update order status for orderId: ".concat(orderId).concat(" to status : ").concat(status.toString()));
            }
        } else if(status.equals(OrderStatus.COMPLETED)){
            try{
                boolean usingDelivery = orderDoc.get(USE_DELIVERY, Boolean.class);
                if(usingDelivery && orderDoc.containsKey(DELIVERY_PARTNER_ID)){
                    //update delivery service about status change if delivery opted for
                    String deliveryPartnerId = orderDoc.get(DELIVERY_PARTNER_ID, String.class);
                    boolean deliveryStatusRes = updateDeliveryStatusforOrder(orderDoc, false,
                            deliveryPartnerId, OrderStatus.COMPLETED);
                    if(!deliveryStatusRes){
                        log.error("Failed to complete delivery for orderId: {} for deliveryPartnerId: {}", orderId, deliveryPartnerId);
                        return utils.getFailedResponse("Failed to complete delivery for order");
                    }
                    log.info("Delivery completed successfully in delivery service for orderId: {} and deliveryPartnerId: {}, updating data in order colls.",
                            orderId, deliveryPartnerId);
                }
                orderDoc.put(STATUS, OrderStatus.COMPLETED);
                orderDoc.put(UPDATED_AT, System.currentTimeMillis());
                orderDoc.put(UPDATED_BY, usingDelivery ? DELIVERY_PARTNER : MERCHANT);
                mongoManager.insertDocument(orderDoc, orderDb, completedOrderColl);
                mongoManager.deleteDocument(query, orderDb, orderColl);

                BigDecimal orderPrice =  mapper.convertValue(orderDoc.get(TOTAL_PRICE), BigDecimal.class);
                updateMerchantEarnings(orderId, orderDoc.get(MERCHANT_ID, String.class),  orderPrice);
                updateCustomerRewardPoints(orderId, orderDoc.get(CUSTOMER_ID, String.class), orderPrice);
                log.info("Order has been completed successfully for orderId: {} and kept in completed coll", orderId);
                return utils.getSuccessResponse("Order has been completed successfully for orderId: ".concat(orderId).concat(" and kept in completed coll"), null);
            }catch(Exception ex){
                log.error("Exception occurred while marking order status for orderId: {} as completed", orderId);
                return utils.getFailedResponse("Exception occurred while marking order status for orderId: ".concat(orderId).concat(" as completed "));
            }
        } else if(status.equals(OrderStatus.CANCELLED)){
            try{
                orderDoc.put(STATUS, OrderStatus.CANCELLED);
                orderDoc.put(UPDATED_AT, System.currentTimeMillis());
                orderDoc.put(UPDATED_BY, MERCHANT);
                mongoManager.insertDocument(orderDoc, orderDb, cancelledOrderColl);
                mongoManager.deleteDocument(query, orderDb, orderColl);
                //this will restore the reward points for user.
                updateCustomerRewardPoints(orderId, orderDoc.get(CUSTOMER_ID, String.class),
                        mapper.convertValue(orderDoc.get("customerRewardsPointsUsed"), BigDecimal.class));
                log.info("Order has been cancelled successfully for orderId: {} and kept in cancelled coll", orderId);
                return utils.getSuccessResponse("Order has been cancelled successfully for orderId: ".concat(orderId).concat(" and kept in cancelled coll"), null);
            }catch(Exception ex){
                log.error("Exception occurred while marking order status for orderId: {} as cancelled", orderId);
                return utils.getFailedResponse("Exception occurred while marking order status for orderId: ".concat(orderId).concat(" as cancelled "));
            }
        } else if(status.equals(OrderStatus.DELIVERY_ACCEPTED)){
            if(!orderDoc.containsKey(USE_DELIVERY) || !orderDoc.get(USE_DELIVERY, Boolean.class)){
                log.error("Attempting to start delivery for orderId: {} without opting for delivery", orderId);
                return utils.getFailedResponse("Attempting to start delivery for order which didnt opt for delivery");
            }
            if(payload==null || !payload.hasNonNull(DELIVERY_PARTNER_ID)){
                log.error("No delivery partner id found in payload {} for orderId: {} while starting delivery", payload, orderId);
                return utils.getFailedResponse("No delivery partner id provided, unable to start delivery");
            }
            String deliveryPartnerId = payload.get(DELIVERY_PARTNER_ID).asText();
            boolean deliveryStatusRes = updateDeliveryStatusforOrder(orderDoc, true, deliveryPartnerId, OrderStatus.DELIVERY_ACCEPTED);
            if(!deliveryStatusRes){
                log.error("Failed to start delivery for orderId: {} for deliveryPartnerId: {}", orderId, deliveryPartnerId);
                return utils.getFailedResponse("Failed to start delivery for order");
            }
            log.info("Delivery record successfully created in delivery service for orderId: {} and deliveryPartnerId: {}, updating data in order colls.",
                    orderId, deliveryPartnerId);
            Document updateDoc = new Document(STATUS, status);
            updateDoc.put(DELIVERY_PARTNER_ID, deliveryPartnerId);
            updateDoc.put(UPDATED_AT, System.currentTimeMillis());
            updateDoc.put(UPDATED_BY, DELIVERY_PARTNER);
            log.info("Starting delivery for order: {} using query : {}", orderId, mapper.convertValue(query, JsonNode.class));
            Document result = mongoManager.findOneAndUpdate(query, new Document("$set", updateDoc), orderDb, orderColl, false, true);
            if(result != null){
                log.info("Started delivery flow successfully for order: {} and kept status : {}", orderId, status);
                return utils.getSuccessResponse("Delivery has been started for the order successfully", null);
            }else{
                log.error("Failed to initiate delivery for order: {},  status : {}", orderId, status);
                return utils.getFailedResponse("Failed to initiate delivery for order");
            }
        } else if(status.equals(OrderStatus.DELIVERY_PICKED_UP)){
            if(!orderDoc.containsKey(USE_DELIVERY) || !orderDoc.get(USE_DELIVERY, Boolean.class) || !orderDoc.containsKey(DELIVERY_PARTNER_ID)){
                log.error("Delivery not yet started for the order: {} or not opted for delivery", orderId);
                return utils.getFailedResponse("Delivery has not yet started for this order or not opted for delivery");
            }
            String deliveryPartnerId = orderDoc.get(DELIVERY_PARTNER_ID, String.class);
            boolean deliveryStatusRes = updateDeliveryStatusforOrder(orderDoc, false, deliveryPartnerId, OrderStatus.DELIVERY_PICKED_UP);
            if(!deliveryStatusRes){
                log.error("Failed to update delivery status for orderId: {} for deliveryPartnerId: {} in delivery service", orderId, deliveryPartnerId);
                return utils.getFailedResponse("Failed to update delivery status for order");
            }
            log.info("Delivery record successfully updated in delivery service for orderId: {} and deliveryPartnerId: {}, updating data in order colls.",
                    orderId, deliveryPartnerId);
            Document updateDoc = new Document(STATUS, status);
            updateDoc.put(UPDATED_AT, System.currentTimeMillis());
            updateDoc.put(UPDATED_BY, DELIVERY_PARTNER);
            log.info("Updating delivery status for order: {} using query : {}", orderId, mapper.convertValue(query, JsonNode.class));
            Document result = mongoManager.findOneAndUpdate(query, new Document("$set", updateDoc), orderDb, orderColl, false, true);
            if(result != null){
                log.info("Successfully update delivery status for order: {} and kept status : {}", orderId, status);
                return utils.getSuccessResponse("Delivery status has been updated for the order successfully", null);
            }else{
                log.error("Failed to update delivery status for order: {},  status : {}", orderId, status);
                return utils.getFailedResponse("Failed to update delivery status for order");
            }
        } else{
            log.error("Invalid status provided for orderId: {}", orderId);
            return utils.getFailedResponse("Invalid status provided for orderId: ".concat(orderId));
        }
    }

    public boolean deleteOrder(String orderId){
        log.info("Deleting order for orderId: {}", orderId);
        Document query = new Document(ORDER_ID, orderId);

        boolean result = mongoManager.deleteDocument(query, orderDb, orderColl);
        if(result){
            log.info("Order deleted successfully for orderId: {}", orderId);
            return true;
        }else{
            log.error("Failed to delete order for orderId: {}", orderId);
            return false;
        }
    }

    private void updateCustomerRewardPoints(String orderId, String customerId, BigDecimal orderPrice){
        log.info("Updating customer reward points for orderId: {} and customerId : {}", orderId, customerId);
        //url is /customers/{customer-id}/rewards/{order-price}
        String url = profileServiceUrl.concat("customers").concat(SLASH).concat(customerId)
                .concat(SLASH).concat("rewards").concat(SLASH).concat(orderPrice.toString());
        log.debug("URL to update customer reward points is : {}", url);
        try{
            Response response = wsUtils.makeWSCallString(url, null, new HashMap<>(), HttpMethod.PUT, 1000, 30000);
            if(FAILURE.equalsIgnoreCase(response.getStatus())){
                log.error("Failed to update customer reward points for orderId: {}", orderId);
            }else{
                log.info("Customer {} reward points updated successfully for orderId: {} with order amount {}",
                        customerId, orderId, orderPrice);
            }
        }catch(Exception ex){
            log.error("Exception occurred while updating customer reward points for orderId: {}", orderId);
        }
    }
    private void updateMerchantEarnings(String orderId, String merchantId, BigDecimal orderPrice){
        log.info("Updating merchant earnings for orderId : {} and merchantId {}", orderId, merchantId);
        //url is /merchants/{merchant-id}/rewards/{order-price}
        String url = profileServiceUrl.concat("merchants").concat(SLASH).concat(merchantId)
                .concat(SLASH).concat("rewards").concat(SLASH).concat(orderPrice.toString());
        log.debug("URL to update merchant earnings is : {}", url);
        try{
            Response response = wsUtils.makeWSCallString(url, null, new HashMap<>(), HttpMethod.PUT, 1000, 30000);
            if(FAILURE.equalsIgnoreCase(response.getStatus())){
                log.error("Failed to update merchant earnings for orderId: {}", orderId);
            }else{
                log.info("Merchant {} earnings updated successfully for orderId: {} with order amount {}",
                        merchantId, orderId, orderPrice);
            }
        }catch(Exception ex){
            log.error("Exception occurred while updating merchant earnings for orderId: {}", orderId);
        }
    }
    private boolean updateDeliveryStatusforOrder(Document orderDoc, boolean isCreateNewDelivery, String deliveryPartnerId, OrderStatus status){
        log.info("Updating delivery status for orderId : {} and deliveryPartnerId {} with status {}", orderDoc.get(ORDER_ID), deliveryPartnerId, status);
        String orderId = orderDoc.get(ORDER_ID, String.class);
        String customerId = orderDoc.get(CUSTOMER_ID, String.class);
        DeliveryStatusReqModel reqModel = new DeliveryStatusReqModel();
        reqModel.setOrderId(orderId);
        reqModel.setCustomerId(customerId);
        reqModel.setDeliveryPersonId(deliveryPartnerId);
        reqModel.setStatus(status);
        reqModel.setMessage("Delivery status updated for orderId: ".concat(orderId).concat(" to status : ").concat(status.toString()));
        String url = deliveryServiceUrl.concat("deliveries");
        HttpMethod method;
        JsonNode payload;
        if(isCreateNewDelivery){
            //to create new delivery : deliveries/?orderId={orderId}&deliveryPersonId={deliveryPartnerId}&customerId={customerId}
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url.concat(SLASH));
            uriBuilder.queryParam(ORDER_ID, orderId);
            uriBuilder.queryParam("deliveryPersonId", deliveryPartnerId);
            uriBuilder.queryParam(CUSTOMER_ID, customerId);
            url=uriBuilder.toUriString();
            method = HttpMethod.POST;
            payload = null;
        }else{
            //to update delivery status : deliveries/status
            url = url.concat(SLASH).concat("status");
            method = HttpMethod.PUT;
            payload = mapper.convertValue(reqModel, JsonNode.class);
        }
        log.debug("URL to update delivery status is : {} with payload {}", url, payload);
        try{
            Response response = wsUtils.makeWSCallObject(url, payload, new HashMap<>(), method, 1000, 30000);
            if(FAILURE.equalsIgnoreCase(response.getStatus())){
                log.error("Failed to update delivery status for orderId: {} for status {}", orderDoc.get(ORDER_ID), status);
                return false;
            }else{
                log.info("Delivery status updated successfully for orderId: {} with deliveryPartnerId {} for status {}",
                        orderDoc.get(ORDER_ID), orderDoc.get(DELIVERY_PARTNER), status);
                return true;
            }
        }catch(Exception ex){
            log.error("Exception occurred while updating delivery status for orderId: {} for status {}", orderDoc.get(ORDER_ID), status);
            return false;
        }
    }
}
