package sg.edu.nus.iss.order_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.core.util.Json;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.order_service.db.MongoManager;
import sg.edu.nus.iss.order_service.model.*;
import sg.edu.nus.iss.order_service.strategy.context.OrderTypeStrategyContext;
import sg.edu.nus.iss.order_service.strategy.context.ProfileTypeStrategyContext;
import sg.edu.nus.iss.order_service.strategy.order.OrderTypeStrategy;
import sg.edu.nus.iss.order_service.strategy.profile.ProfileTypeStrategy;
import sg.edu.nus.iss.order_service.states.*;
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
    private final OrderTypeStrategyContext orderTypeStrategyContext;
    private final ProfileTypeStrategyContext profileTypeStrategyContext;
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

    private OrderTypeStrategy completedOrderStrategy;


    private OrderTypeStrategy activeOrderStrategy;


    private OrderTypeStrategy cancelledOrderStrategy;


    private OrderTypeStrategy allOrderStrategy;


    private ProfileTypeStrategy customerProfileStrategy;


    private ProfileTypeStrategy merchantProfileStrategy;


    private ProfileTypeStrategy deliveryPartnerProfileStrategy;

    @Autowired
    public OrderService(CartService cartService, MongoManager mongoManager, WSUtils wsUtils, Utils utils,
                        OrderTypeStrategyContext orderTypeStrategyContext,
                        ProfileTypeStrategyContext profileTypeStrategyContext,
                        @Qualifier("COMPLETED") OrderTypeStrategy completedOrderStrategy,
                        @Qualifier("ACTIVE") OrderTypeStrategy activeOrderStrategy,
                        @Qualifier("CANCELLED") OrderTypeStrategy cancelledOrderStrategy,
                        @Qualifier("ALL") OrderTypeStrategy allOrderStrategy,
                        @Qualifier("customer") ProfileTypeStrategy customerProfileStrategy,
                        @Qualifier("merchant") ProfileTypeStrategy merchantProfileStrategy,
                        @Qualifier("deliveryPartner") ProfileTypeStrategy deliveryPartnerProfileStrategy) {
        this.cartService = cartService;
        this.mongoManager = mongoManager;
        this.wsUtils = wsUtils;
        this.utils = utils;
        this.orderTypeStrategyContext = orderTypeStrategyContext;
        this.profileTypeStrategyContext = profileTypeStrategyContext;
        this.completedOrderStrategy = completedOrderStrategy;
        this.activeOrderStrategy = activeOrderStrategy;
        this.allOrderStrategy = allOrderStrategy;
        this.cancelledOrderStrategy = cancelledOrderStrategy;
        this.customerProfileStrategy = customerProfileStrategy;
        this.merchantProfileStrategy = merchantProfileStrategy;
        this.deliveryPartnerProfileStrategy = deliveryPartnerProfileStrategy;
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
            JsonNode rewrdsObject = utils.getRewardPointsOffsetForCustomer(order.getCustomerId());
            if(rewrdsObject!=null){
                BigDecimal rewardPointsAmountOffset = rewrdsObject.get("rewardAmount").decimalValue();
                BigDecimal rewardsPoints = rewrdsObject.get("rewardPoints").decimalValue();
                order.setRewardsAmountUsed(rewardPointsAmountOffset);
                order.setCustomerRewardsPointsUsed(rewardsPoints);
                order.setTotalPrice(order.getTotalPrice().subtract(rewardPointsAmountOffset));
                utils.updateCustomerRewardPoints(order.getOrderId(), order.getCustomerId(), BigDecimal.ZERO);
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
                //this url has now changed where merchant id is now needed as request params.
//                String reqUrl = url.concat(SLASH).concat(reqProd.getMerchantId().toString()).concat(SLASH)
//                        .concat("products").concat(SLASH).concat(reqProd.getProductId().toString());
                String reqUrl = url.concat(SLASH).concat("products").concat(SLASH).concat(reqProd.getProductId().toString());
                UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(reqUrl);
                uriBuilder.queryParam("user-id", reqProd.getMerchantId().toString());
                reqUrl = uriBuilder.toUriString();
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

    private BigDecimal calculateTotalPrice(List<Item> cartItems) {
        return cartItems.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Response getOrdersListByProfileId(String listType, String profileType, String id) {

        try {
            // Select the appropriate order strategy based on listType
            OrderTypeStrategy orderTypeStrategy;
            switch (listType.toUpperCase()) {
                case "COMPLETED":
                    orderTypeStrategy = completedOrderStrategy;
                    log.info("Selected order strategy: COMPLETED");
                    break;
                case "ACTIVE":
                    orderTypeStrategy = activeOrderStrategy;
                    log.info("Selected order strategy: ACTIVE");
                    break;
                case "CANCELLED":
                    orderTypeStrategy = cancelledOrderStrategy;
                    log.info("Selected order strategy: CANCELLED");
                    break;
                default:
                    orderTypeStrategy = allOrderStrategy;
                    log.info("Selected order strategy: ALL");
                    break;
            }

            String formattedId = utils.getProfileIdentifierFieldBasedOnRole(profileType);
            if (formattedId.isEmpty()) {
                return utils.getFailedResponse("Invalid profile type: " + profileType); // Return failure response for invalid profileType
            }

            // Fetch orders using the selected order strategy
            Response ordersData = orderTypeStrategy.getOrders(profileType, id); // Assuming this returns Response, not JsonNode

            // Check if no orders were found
            if (ordersData == null || ordersData.getData() == null || ordersData.getData().isEmpty()) {
                return utils.getFailedResponse("No orders found for profileType " + profileType + " and ID " + id);
            }

            // Log success and return the successful response
            return utils.getSuccessResponse("Orders retrieved successfully", ordersData.getData());

        } catch (Exception e) {
            // Handle exceptions gracefully
            log.error("An error occurred while retrieving orders for profileType {} and ID {}: {}", profileType, id, e.getMessage(), e);
            return utils.getFailedResponse("An error occurred while retrieving orders: " + e.getMessage()); // Return failure response on error
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

    public Response updateOrderStatus(String orderId, OrderStatus status, JsonNode payload){
        log.info("Updating order status for orderId: {} to status : {}", orderId, status);
        Document query = new Document(ORDER_ID, orderId);
        Document orderDoc = mongoManager.findDocument(query, orderDb, orderColl);
        if(orderDoc == null){
            log.error("No order found for orderId: {}", orderId);
            return utils.getFailedResponse("No order found for orderId: ".concat(orderId));
        }
        try {
            OrderContext context = new OrderContext();
            context.setOrderDoc(orderDoc);
            context.setOrderId(orderId);
            context.setPayload(payload);
            context.setCurrentState(getStateForStatus(status));
            return context.updateStatus();
        } catch (IllegalArgumentException ex){
            log.error("Invalid status provided for orderId: {}", orderId);
            return utils.getFailedResponse("Invalid status provided for orderId: ".concat(orderId));
        }catch(Exception ex) {
            log.error("Exception occurred while updating order status for orderId: {}", orderId, ex);
            return utils.getFailedResponse("Exception occurred updating order status");
        }
    }

    private OrderState getStateForStatus(OrderStatus status) {
        switch (status) {
            case ACCEPTED:
                return new AcceptedState(mongoManager, utils, orderDb, orderColl);
            case READY:
                return new ReadyState(mongoManager, utils, orderDb, orderColl);
            case DELIVERY_ACCEPTED:
                return new DeliveryAcceptedState(mongoManager, utils, orderDb, orderColl);
            case DELIVERY_PICKED_UP:
                return new DeliveryPickedUpState(mongoManager, utils, orderDb, orderColl);
            case COMPLETED:
                return new CompletedState(mongoManager, utils, orderDb, orderColl, completedOrderColl);
            case CANCELLED:
                return new CancelledState(mongoManager, utils, orderDb, orderColl, cancelledOrderColl);
            default:
                throw new IllegalArgumentException("Invalid status: " + status);
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
}
