package sg.edu.nus.iss.order_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.core.util.Json;
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
import sg.edu.nus.iss.order_service.utils.WSUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService extends Constants {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final ObjectMapper mapper = Json.mapper();

    private final CartService cartService;
    private final MongoManager mongoManager;
    private final WSUtils wsUtils;

    @Value("${"+ORDER_DB+"}")
    private String orderDb;

    @Value("${"+ORDER_COLLECTION+"}")
    private String orderColl;

    @Value("${"+COMPLETED_ORDERS_COLL+"}")
    private String completedOrderColl;

    @Value("${"+CANCELLED_ORDERS_COLL+"}")
    private String cancelledOrderColl;

    @Value("${product.service.url}")
    private String productServiceUrl; //http://localhost:8080/products/ids with query param as "productIds"

    @Autowired
    public OrderService(CartService cartService, MongoManager mongoManager, WSUtils wsUtils) {
        this.cartService = cartService;
        this.mongoManager = mongoManager;
        this.wsUtils = wsUtils;
    }

    public String createOrderFromCart(String customerId){
        log.info("Creating order for customer: {}", customerId);
        Cart cart = cartService.getCartByCustomerId(customerId);
        if(cart == null || cart.getCartItems().isEmpty()){
            log.error("No items found in cart for customer: {}, so no order to be created", customerId);
            return null;
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

        List<Item> cartItemsWithPrice = getProductDetailsForItems(cart.getCartItems());
        if(cartItemsWithPrice == null || cartItemsWithPrice.isEmpty()){
            log.error("Failed to get product details or found not matching products found for items in cart for customer: {}", customerId);
            return "";
        }
        order.setOrderItems(cartItemsWithPrice);
        order.setTotalPrice(calculateTotalPrice(cartItemsWithPrice));

        log.info("Order to be created : {}", order);

        Document insertDocument = mapper.convertValue(order, Document.class);
        boolean result = mongoManager.insertDocument(insertDocument, orderDb, orderColl);
        if(result){
            log.info("Order created successfully for customer: {}", customerId);
            //raise create event here.
            //make async call to update Product-service for product's stock
            cartService.deleteCartByCustomerId(customerId);
            return "Order created successfully for customer: "+customerId;
        }else{
            log.error("Failed to create order for customer: {}", customerId);
            return "";
        }
    }

    private List<Item> getProductDetailsForItems(List<Item> cartItems){
        String prodIds = "";
        for(Item item : cartItems){
            prodIds = prodIds.concat(item.getProductId().toString()).concat(",");
        }
        prodIds = prodIds.substring(0, prodIds.length()-1);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(productServiceUrl);
        uriBuilder.queryParam("productIds", prodIds);

        Response response = wsUtils.makeWSCallObject(uriBuilder.toUriString(), null, new HashMap<>(), HttpMethod.GET, 1000, 30000);
        if(FAILURE.equalsIgnoreCase(response.getStatus())){
            log.error("Failed to get product details for productIds: {}", prodIds);
            return null;
        }
        List<Item> itemsForOrder = new ArrayList<>();
        ArrayNode data = (ArrayNode) response.getData();
        for(int i =0 ; i<data.size(); i++){
            for(int j=0; j<cartItems.size(); j++){
                JsonNode itemNode = data.get(i);
                Item item = cartItems.get(j);
                log.debug("in this iteration checking itemNode : {} and item : {}", itemNode, item);
                if(itemNode.hasNonNull("productId") && item.getProductId().toString().equalsIgnoreCase(itemNode.get("productId").textValue())
                        && itemNode.hasNonNull("listingPrice")){
                    item.setPrice(new BigDecimal(itemNode.get("listingPrice").textValue()));
                    itemsForOrder.add(item);
                }
            }
        }
        return itemsForOrder;
    }

    private BigDecimal calculateTotalPrice(List<Item> cartItems) {
        return cartItems.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Order> getCompletedOrdersByProfileId(String profileId, String profileIdKey){
        log.info("Fetching completed orders for profileId: {}", profileId);
        Document query = new Document(profileIdKey, profileId);
        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, completedOrderColl);
        if(orders!=null && !orders.isEmpty()){
            log.info("Found completed orders for profileKey {}, profileId: {}, count {}", profileIdKey, profileId, orders.size());
            return mapper.convertValue(orders, List.class);
        }else{
            log.info("No completed orders found for profileKey {}, profileId: {}", profileIdKey, profileId);
            return null;
        }
    }

    public List<Order> getCancelledOrdersByProfileId(String profileId, String profileIdKey){
        log.info("Fetching cancelled orders for profileId: {}", profileId);
        Document query = new Document(profileIdKey, profileId);
        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, cancelledOrderColl);
        if(orders!=null && !orders.isEmpty()){
            log.info("Found cancelled orders for profileKey {}, profileId: {}, count {}", profileIdKey, profileId, orders.size());
            return mapper.convertValue(orders, List.class);
        }else{
            log.info("No cancelled orders found for profileKey {}, profileId: {}", profileIdKey, profileId);
            return null;
        }
    }

    public List<Order> getActiveOrdersByProfileId(String profileId, String profileIdKey){
        log.info("Fetching active orders for profileId: {}", profileId);
        Document query = new Document(profileIdKey, profileId);
        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, orderColl);
        if(orders!=null && !orders.isEmpty()){
            log.info("Found active orders for profileKey {}, profileId: {}, count {}", profileIdKey, profileId, orders.size());
            return mapper.convertValue(orders, List.class);
        }else{
            log.info("No active orders found for profileKey {}, profileId: {}", profileIdKey, profileId);
            return null;
        }
    }

    public List<Order> getAllOrdersByProfileId(String profileId, String profileIdKey){
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
        return mapper.convertValue(totalOrders, List.class);
    }

    public Order getOrderByOrderId(String orderId){
        log.info("Fetching order by orderId: {}", orderId);
        Document query = new Document(ORDER_ID, orderId);
        Document resDoc = mongoManager.findDocument(query, orderDb, orderColl);
        if(resDoc!=null && !resDoc.isEmpty()){
            log.info("Found orders for provided orderId: {}", orderId);
            return mapper.convertValue(resDoc, Order.class);
        }else{
            log.info("No orders found for orderId: {}", orderId);
            return null;
        }
    }

    //later on we need to implement Chain of Responsibility pattern to handle this.
    public boolean updateOrderStatus(String orderId, OrderStatus status){
        log.info("Updating order status for orderId: {} to status : {}", orderId, status);
        Document query = new Document(ORDER_ID, orderId);
        Document orderDoc = mongoManager.findDocument(query, orderDb, orderColl);
        if(orderDoc == null){
            log.error("No order found for orderId: {}", orderId);
            return false;
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
                return true;
            }else{
                log.error("Failed to update order status for orderId: {} to status : {}", orderId, status);
                return false;
            }
        } else if(status.equals(OrderStatus.COMPLETED)){
            //move document to completed-order collections
            //then delete from order coll.
            orderDoc.put(STATUS, OrderStatus.COMPLETED);
            orderDoc.put(UPDATED_AT, System.currentTimeMillis());
            orderDoc.put(UPDATED_BY, MERCHANT);
            mongoManager.insertDocument(orderDoc, orderDb, completedOrderColl);
            mongoManager.deleteDocument(query, orderDb, orderColl);
            log.info("Order has been completed successfully for orderId: {} and kept in completed coll", orderId);
            return true;
        } else if(status.equals(OrderStatus.CANCELLED)){
            //move document to cancelled-order collections
            //then delete from order coll.
            orderDoc.put(STATUS, OrderStatus.CANCELLED);
            orderDoc.put(UPDATED_AT, System.currentTimeMillis());
            orderDoc.put(UPDATED_BY, MERCHANT);
            mongoManager.insertDocument(orderDoc, orderDb, completedOrderColl);
            mongoManager.deleteDocument(query, orderDb, orderColl);
            log.info("Order has been cancelled successfully for orderId: {} and kept in cancelled coll", orderId);
            return true;
        } else{
            log.error("Invalid status provided for orderId: {}", orderId);
            return false;
        }
    }


}
