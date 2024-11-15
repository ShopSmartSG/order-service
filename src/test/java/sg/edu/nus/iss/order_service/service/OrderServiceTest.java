package sg.edu.nus.iss.order_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.order_service.db.MongoManager;
import sg.edu.nus.iss.order_service.model.*;
import sg.edu.nus.iss.order_service.utils.ApplicationConstants;
import sg.edu.nus.iss.order_service.utils.Constants;
import sg.edu.nus.iss.order_service.utils.Utils;
import sg.edu.nus.iss.order_service.utils.WSUtils;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class OrderServiceTest extends Constants {
    private final ObjectMapper objectMapper = Json.mapper();
    private UUID globalUUID = UUID.randomUUID();
    @Mock
    ObjectMapper mapper;
    @Mock
    CartService cartService;
    @Mock
    MongoManager mongoManager;
    @Mock
    Utils utils;
    @Mock
    WSUtils wsUtils;

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

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateOrderFromCart_CartNotFound(){
        when(cartService.getCartByCustomerId(anyString())).thenReturn(null);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Cart not found"));
        Response response = orderService.createOrderFromCart("customer1", true, true);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Cart not found", response.getMessage());
    }
    @Test
    public void testCreateOrderFromCart_Success() {
        Category cat = new Category();
        cat.setCategoryId(globalUUID);
        cat.setCategoryName("cat1");
        cat.setCategoryDescription("cat1 desc");

        Product product = new Product();
        product.setProductId(globalUUID);
        product.setMerchantId(globalUUID);
        product.setListingPrice(BigDecimal.valueOf(10.0));
        product.setCategory(cat);
        product.setAvailableStock(10);

        List<Product> products = new ArrayList<>();
        products.add(product);
        Response prodDetailsResponse = new Response();
        prodDetailsResponse.setStatus(SUCCESS);
        prodDetailsResponse.setData(objectMapper.convertValue(products, ArrayNode.class));

        ObjectNode rewards = objectMapper.createObjectNode();
        rewards.put("rewardAmount", BigDecimal.valueOf(5.0));
        rewards.put("rewardPoints", BigDecimal.valueOf(500.0));
        Response rewardsResponse = new Response();
        rewardsResponse.setStatus(SUCCESS);
        rewardsResponse.setData(rewards);

        Response successResp = new Response();
        successResp.setStatus(SUCCESS);

        String productIdsListUrl = productServiceUrl.concat("products/ids?productIds=").concat(globalUUID.toString());
        String rewardPointsUrl = profileServiceUrl.concat("customers").concat(SLASH).concat("customer1").concat(SLASH).concat("rewards");
        String updateProductUrl = productServiceUrl.concat("merchants").concat(SLASH).concat(globalUUID.toString()).concat(SLASH)
                .concat("products").concat(SLASH).concat(globalUUID.toString());
        String profileCustomerUpdate = profileServiceUrl.concat("customers").concat(SLASH).concat("customer1")
                .concat(SLASH).concat("rewards").concat(SLASH).concat(BigDecimal.valueOf(0).toString());

        when(cartService.getCartByCustomerId(anyString())).thenReturn(getCartDocument());
        when(mongoManager.insertDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(true);
        when(wsUtils.makeWSCallObject(eq(productIdsListUrl), any(),any(), any(), anyLong(), anyLong())).thenReturn(prodDetailsResponse);
        when(wsUtils.makeWSCallObject(eq(rewardPointsUrl), any(),any(), any(), anyLong(), anyLong())).thenReturn(rewardsResponse);
        when(wsUtils.makeWSCallObject(eq(updateProductUrl), any(), any(), any(), anyLong(), anyLong())).thenReturn(successResp);
        when(wsUtils.makeWSCallString(eq(profileCustomerUpdate), any(), any(), any(), anyLong(), anyLong())).thenReturn(successResp);
        when(cartService.deleteCartByCustomerId(anyString())).thenReturn(null);
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Order created", objectMapper.createObjectNode()));
        Response response = orderService.createOrderFromCart("customer1", true, true);
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Order created", response.getMessage());
    }
    @Test
    public void testCreateOrderFromCart_Failure_NoProductsFound(){
        Response prodDetailsResponse = new Response();
        prodDetailsResponse.setStatus(FAILURE);

        String productIdsListUrl = productServiceUrl.concat("products/ids?productIds=").concat(globalUUID.toString());

        when(cartService.getCartByCustomerId(anyString())).thenReturn(getCartDocument());
        when(mongoManager.insertDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(true);
        when(wsUtils.makeWSCallObject(eq(productIdsListUrl), any(),any(), any(), anyLong(), anyLong())).thenReturn(prodDetailsResponse);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("No products found"));
        Response response = orderService.createOrderFromCart("customer1", true, true);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("No products found", response.getMessage());
    }

    @Test
    public void testCreateOrderFromCart_Failure_ProductUpdateFailed(){
        Category cat = new Category();
        cat.setCategoryId(globalUUID);
        cat.setCategoryName("cat1");
        cat.setCategoryDescription("cat1 desc");

        Product product = new Product();
        product.setProductId(globalUUID);
        product.setMerchantId(globalUUID);
        product.setListingPrice(BigDecimal.valueOf(10.0));
        product.setCategory(cat);
        product.setAvailableStock(10);

        List<Product> products = new ArrayList<>();
        products.add(product);
        Response prodDetailsResponse = new Response();
        prodDetailsResponse.setStatus(SUCCESS);
        prodDetailsResponse.setData(objectMapper.convertValue(products, ArrayNode.class));

        Response updateProdResponse = new Response();
        updateProdResponse.setStatus(FAILURE);

        String productIdsListUrl = productServiceUrl.concat("products/ids?productIds=").concat(globalUUID.toString());
        String updateProductUrl = productServiceUrl.concat("merchants").concat(SLASH).concat(globalUUID.toString()).concat(SLASH)
                .concat("products").concat(SLASH).concat(globalUUID.toString());
        when(cartService.getCartByCustomerId(anyString())).thenReturn(getCartDocument());
        when(mongoManager.insertDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(true);
        when(wsUtils.makeWSCallObject(eq(productIdsListUrl), any(),any(), any(), anyLong(), anyLong())).thenReturn(prodDetailsResponse);
        when(wsUtils.makeWSCallObject(eq(updateProductUrl), any(), any(), any(), anyLong(), anyLong())).thenReturn(updateProdResponse);
        when(cartService.deleteCartByCustomerId(anyString())).thenReturn(null);
        when(mongoManager.deleteDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(true);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Failed to create order"));
        Response response = orderService.createOrderFromCart("customer1", false, true);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to create order", response.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_Failure_UnknownProfileType() {
        when(utils.getProfileIdentifierFieldBasedOnRole(anyString())).thenReturn("");
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Unknown profile type"));
        Response response = orderService.getOrdersListByProfileId("all", "newType", "new1");
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Unknown profile type", response.getMessage());
    }
    @Test
    public void testGetOrdersListByProfileId_Completed_Success() {
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(completedOrderColl))).thenReturn(List.of(getOrderDocument_withDelivery()));
        when(utils.getProfileIdentifierFieldBasedOnRole(anyString())).thenReturn(MERCHANT_ID);
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Orders found", objectMapper.createObjectNode()));
        Response response = orderService.getOrdersListByProfileId(COMPLETED, MERCHANT, "merchant1");
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Orders found", response.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_Completed_Failure() {
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(completedOrderColl))).thenReturn(new ArrayList<>());
        when(utils.getProfileIdentifierFieldBasedOnRole(anyString())).thenReturn(MERCHANT_ID);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("No orders found"));
        Response response = orderService.getOrdersListByProfileId(COMPLETED, MERCHANT, "merchant1");
        assertEquals(FAILURE, response.getStatus());
        assertEquals("No orders found", response.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_Cancelled_Success(){
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(cancelledOrderColl))).thenReturn(List.of(getOrderDocument_withDelivery()));
        when(utils.getProfileIdentifierFieldBasedOnRole(anyString())).thenReturn(MERCHANT_ID);
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Orders found", objectMapper.createObjectNode()));
        Response response = orderService.getOrdersListByProfileId(CANCELLED, MERCHANT, "merchant1");
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Orders found", response.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_Cancelled_Failure(){
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(cancelledOrderColl))).thenReturn(new ArrayList<>());
        when(utils.getProfileIdentifierFieldBasedOnRole(anyString())).thenReturn(MERCHANT_ID);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("No orders found"));
        Response response = orderService.getOrdersListByProfileId(CANCELLED, MERCHANT, "merchant1");
        assertEquals(FAILURE, response.getStatus());
        assertEquals("No orders found", response.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_Active_Success(){
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(orderColl))).thenReturn(List.of(getOrderDocument_withDelivery()));
        when(utils.getProfileIdentifierFieldBasedOnRole(anyString())).thenReturn(MERCHANT_ID);
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Orders found", objectMapper.createObjectNode()));
        Response response = orderService.getOrdersListByProfileId(ACTIVE, MERCHANT, "merchant1");
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Orders found", response.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_Active_Failure(){
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(orderColl))).thenReturn(new ArrayList<>());
        when(utils.getProfileIdentifierFieldBasedOnRole(anyString())).thenReturn(MERCHANT_ID);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("No orders found"));
        Response response = orderService.getOrdersListByProfileId(ACTIVE, MERCHANT, "merchant1");
        assertEquals(FAILURE, response.getStatus());
        assertEquals("No orders found", response.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_All_Success(){
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(orderColl))).thenReturn(List.of(getOrderDocument_withDelivery()));
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(completedOrderColl))).thenReturn(List.of(getOrderDocument_withDelivery()));
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(cancelledOrderColl))).thenReturn(List.of(getOrderDocument_withDelivery()));
        when(utils.getProfileIdentifierFieldBasedOnRole(anyString())).thenReturn(MERCHANT_ID);
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Orders found", objectMapper.createObjectNode()));
        Response response = orderService.getOrdersListByProfileId("all", MERCHANT, "merchant1");
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Orders found", response.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_All_Failure(){
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(orderColl))).thenReturn(new ArrayList<>());
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(completedOrderColl))).thenReturn(new ArrayList<>());
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(cancelledOrderColl))).thenReturn(new ArrayList<>());
        when(utils.getProfileIdentifierFieldBasedOnRole(anyString())).thenReturn(MERCHANT_ID);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("No orders found"));
        Response response = orderService.getOrdersListByProfileId("all", MERCHANT, "merchant1");
        assertEquals(FAILURE, response.getStatus());
        assertEquals("No orders found", response.getMessage());
    }

    @Test
    public void testGetActiveOrdersForDelivery_Success() {
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(orderColl))).thenReturn(List.of(getOrderDocument_withDelivery()));
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Orders found", objectMapper.createObjectNode()));
        Response response = orderService.getActiveOrdersForDelivery();
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Orders found", response.getMessage());
    }

    @Test
    public void testGetActiveOrdersForDelivery_Failure() {
        when(mongoManager.findAllDocuments(any(), eq(orderDb), eq(orderColl))).thenReturn(new ArrayList<>());
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("No orders found"));
        Response response = orderService.getActiveOrdersForDelivery();
        assertEquals(FAILURE, response.getStatus());
        assertEquals("No orders found", response.getMessage());
    }

    @Test
    public void testGetOrderByOrderId_Success() {
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withDelivery());
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Order found", objectMapper.createObjectNode()));
        Response response = orderService.getOrderByOrderId("order1");
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Order found", response.getMessage());
    }

    @Test
    public void testGetOrderByOrderId_Failure() {
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(null);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Order not found"));
        Response response = orderService.getOrderByOrderId("order1");
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Order not found", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_InvalidStatus() {
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withoutDelivery());
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Invalid status"));
        Response response = orderService.updateOrderStatus("order1", OrderStatus.INVALID, null);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Invalid status", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_NoOrder(){
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(null);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Order not found"));
        Response response = orderService.updateOrderStatus("order1", OrderStatus.READY, null);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Order not found", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_ReadyStatus_Success(){
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withoutDelivery());
        when(mongoManager.findOneAndUpdate(any(), any(), eq(orderDb), eq(orderColl), anyBoolean(), anyBoolean())).thenReturn(getOrderDocument_withoutDelivery());
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Order status updated", objectMapper.createObjectNode()));
        Response response = orderService.updateOrderStatus("order1", OrderStatus.READY, null);
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Order status updated", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_ReadyStatus_Failure(){
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withoutDelivery());
        when(mongoManager.findOneAndUpdate(any(), any(), eq(orderDb), eq(orderColl), anyBoolean(), anyBoolean())).thenReturn(null);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Failed to update order status"));
        Response response = orderService.updateOrderStatus("order1", OrderStatus.READY, null);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to update order status", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_CompletedStatus_FailedDeliveryUpdate(){
        Response delUpdate = new Response();
        delUpdate.setStatus(FAILURE);
        String deliveryUpdateUrl = deliveryServiceUrl.concat("deliveries").concat(SLASH).concat("status");
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withDelivery());
        when(wsUtils.makeWSCallObject(eq(deliveryUpdateUrl), any(), any(), any(), anyLong(), anyLong())).thenReturn(delUpdate);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Failed to update delivery status"));
        Response response = orderService.updateOrderStatus("order1", OrderStatus.COMPLETED, null);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to update delivery status", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_CompletedStatus_Success(){
        Response successResp = new Response();
        successResp.setStatus(SUCCESS);
        String deliveryUpdateUrl = deliveryServiceUrl.concat("deliveries").concat(SLASH).concat("status");
        String profileMerchantUpdate = profileServiceUrl.concat("merchants").concat(SLASH).concat(globalUUID.toString())
                .concat(SLASH).concat("rewards").concat(SLASH).concat(BigDecimal.valueOf(100.0).toString());
        String profileCustomerUpdate = profileServiceUrl.concat("customers").concat(SLASH).concat(globalUUID.toString())
                .concat(SLASH).concat("rewards").concat(SLASH).concat(BigDecimal.valueOf(100.0).toString());
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withDelivery());
        when(wsUtils.makeWSCallObject(eq(deliveryUpdateUrl), any(), any(), any(), anyLong(), anyLong())).thenReturn(successResp);
        when(mongoManager.insertDocument(any(), eq(orderDb), eq(completedOrderColl))).thenReturn(true);
        when(mongoManager.deleteDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(true);
        when(wsUtils.makeWSCallString(eq(profileMerchantUpdate), any(), any(), any(), anyLong(), anyLong())).thenReturn(successResp);
        when(wsUtils.makeWSCallString(eq(profileCustomerUpdate), any(), any(), any(), anyLong(), anyLong())).thenReturn(successResp);
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Order status updated to completed", objectMapper.createObjectNode()));
        Response response = orderService.updateOrderStatus("order1", OrderStatus.COMPLETED, null);
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Order status updated to completed", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_DelPickedUpStatus_FailedDeliveryUpdate(){
        Response delUpdate = new Response();
        delUpdate.setStatus(FAILURE);
        String deliveryUpdateUrl = deliveryServiceUrl.concat("deliveries").concat(SLASH).concat("status");
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withDelivery());
        when(wsUtils.makeWSCallObject(eq(deliveryUpdateUrl), any(), any(), any(), anyLong(), anyLong())).thenReturn(delUpdate);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Failed to update delivery status"));
        Response response = orderService.updateOrderStatus("order1", OrderStatus.DELIVERY_PICKED_UP, null);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to update delivery status", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_DelPickedUpStatus_Success(){
        Response successResp = new Response();
        successResp.setStatus(SUCCESS);
        String deliveryUpdateUrl = deliveryServiceUrl.concat("deliveries").concat(SLASH).concat("status");
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withDelivery());
        when(wsUtils.makeWSCallObject(eq(deliveryUpdateUrl), any(), any(), any(), anyLong(), anyLong())).thenReturn(successResp);
        when(mongoManager.findOneAndUpdate(any(), any(), eq(orderDb), eq(orderColl), anyBoolean(), anyBoolean())).thenReturn(getOrderDocument_withDelivery());
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Order status updated", objectMapper.createObjectNode()));
        Response response = orderService.updateOrderStatus("order1", OrderStatus.DELIVERY_PICKED_UP, null);
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Order status updated", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_DelAcceptedStatus_FailedDeliveryUpdate(){
        Response delUpdate = new Response();
        delUpdate.setStatus(FAILURE);
        String deliveryUpdateUrl = deliveryServiceUrl.concat("deliveries");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(deliveryUpdateUrl.concat(SLASH));
        uriBuilder.queryParam(ORDER_ID, globalUUID.toString());
        uriBuilder.queryParam("deliveryPersonId", globalUUID.toString());
        uriBuilder.queryParam(CUSTOMER_ID, globalUUID.toString());
        deliveryUpdateUrl=uriBuilder.toUriString();
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withDelivery());
        when(wsUtils.makeWSCallObject(eq(deliveryUpdateUrl), any(), any(), any(), anyLong(), anyLong())).thenReturn(delUpdate);
        when(utils.getFailedResponse(anyString())).thenReturn(getMockedFailedResponse("Failed to update delivery status"));

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put(DELIVERY_PARTNER_ID, globalUUID.toString());

        Response response = orderService.updateOrderStatus("order1", OrderStatus.DELIVERY_ACCEPTED, payload);
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to update delivery status", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_DelAcceptedStatus_Success(){
        Response delUpdate = new Response();
        delUpdate.setStatus(SUCCESS);
        String deliveryUpdateUrl = deliveryServiceUrl.concat("deliveries");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(deliveryUpdateUrl.concat(SLASH));
        uriBuilder.queryParam(ORDER_ID, globalUUID.toString());
        uriBuilder.queryParam("deliveryPersonId", globalUUID.toString());
        uriBuilder.queryParam(CUSTOMER_ID, globalUUID.toString());
        deliveryUpdateUrl=uriBuilder.toUriString();
        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withDelivery());
        when(wsUtils.makeWSCallObject(eq(deliveryUpdateUrl), any(), any(), any(), anyLong(), anyLong())).thenReturn(delUpdate);
        when(mongoManager.findOneAndUpdate(any(), any(), eq(orderDb), eq(orderColl), anyBoolean(), anyBoolean())).thenReturn(getOrderDocument_withDelivery());
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Order status updated", objectMapper.createObjectNode()));

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put(DELIVERY_PARTNER_ID, globalUUID.toString());

        Response response = orderService.updateOrderStatus("order1", OrderStatus.DELIVERY_ACCEPTED, payload);
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Order status updated", response.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_Cancelled_Success(){
        Response successResp = new Response();
        successResp.setStatus(SUCCESS);
        String profileCustomerUpdate = profileServiceUrl.concat("customers").concat(SLASH).concat(globalUUID.toString())
                .concat(SLASH).concat("rewards").concat(SLASH).concat(BigDecimal.valueOf(500.0).toString());

        when(mongoManager.findDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(getOrderDocument_withoutDelivery());
        when(mongoManager.insertDocument(any(), eq(orderDb), eq(cancelledOrderColl))).thenReturn(true);
        when(mongoManager.deleteDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(true);
        when(wsUtils.makeWSCallString(eq(profileCustomerUpdate), any(), any(), any(), anyLong(), anyLong())).thenReturn(successResp);
        when(utils.getSuccessResponse(anyString(), any())).thenReturn(getMockedSuccessResponse("Order status updated to cancelled", objectMapper.createObjectNode()));
        Response response = orderService.updateOrderStatus("order1", OrderStatus.CANCELLED, null);
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Order status updated to cancelled", response.getMessage());
    }

    @Test
    public void testDeleteOrder_Success() {
        when(mongoManager.deleteDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(true);
        boolean result = orderService.deleteOrder("order1");
        assertTrue(result);
    }

    @Test
    public void testDeleteOrder_Failure() {
        when(mongoManager.deleteDocument(any(), eq(orderDb), eq(orderColl))).thenReturn(false);
        boolean result = orderService.deleteOrder("order1");
        assertFalse(result);
    }

    private Cart getCartDocument() {
        List<Item> cartItems = new ArrayList<>();
        cartItems.add(new Item(globalUUID.toString(),10));
        Cart cart = new Cart();
        cart.setCustomerId(globalUUID.toString());
        cart.setMerchantId(globalUUID.toString());
        cart.setCartItems(cartItems);
        cart.setCreatedAt(System.currentTimeMillis());
        cart.setUpdatedAt(System.currentTimeMillis());

        return cart;
    }

    private Document getOrderDocument_withDelivery() {
        List<Item> orderItems = new ArrayList<>();
        Item item1 = new Item(globalUUID.toString(),10);
        item1.setPrice(BigDecimal.valueOf(10.0));
        orderItems.add(item1);

        Document orderDocument = new Document();
        orderDocument.put(ORDER_ID, globalUUID.toString());
        orderDocument.put(CUSTOMER_ID, globalUUID.toString());
        orderDocument.put(MERCHANT_ID, globalUUID.toString());
        orderDocument.put(DELIVERY_PARTNER_ID, globalUUID.toString());
        orderDocument.put(ORDER_ITEMS, orderItems);
        orderDocument.put(TOTAL_PRICE, BigDecimal.valueOf(100.0));
        orderDocument.put(STATUS, OrderStatus.DELIVERY_PICKED_UP);
        orderDocument.put(CREATED_AT, System.currentTimeMillis());
        orderDocument.put(UPDATED_AT, System.currentTimeMillis());
        orderDocument.put(CREATED_BY, CUSTOMER);
        orderDocument.put(UPDATED_BY, DELIVERY_PARTNER);
        orderDocument.put(USE_DELIVERY, true);
        orderDocument.put("useRewards", true);
        orderDocument.put("rewardsAmountUsed", BigDecimal.valueOf(5.0));
        orderDocument.put("customerRewardsPointsUsed", BigDecimal.valueOf(500.0));
        return orderDocument;
    }
    private Document getOrderDocument_withoutDelivery() {
        List<Item> orderItems = new ArrayList<>();
        Item item1 = new Item(globalUUID.toString(),10);
        item1.setPrice(BigDecimal.valueOf(10.0));
        orderItems.add(item1);

        Document orderDocument = new Document();
        orderDocument.put(ORDER_ID, globalUUID.toString());
        orderDocument.put(CUSTOMER_ID, globalUUID.toString());
        orderDocument.put(MERCHANT_ID, globalUUID.toString());
        orderDocument.put(ORDER_ITEMS, orderItems);
        orderDocument.put(STATUS, OrderStatus.READY);
        orderDocument.put(TOTAL_PRICE, BigDecimal.valueOf(100.0));
        orderDocument.put(CREATED_AT, System.currentTimeMillis());
        orderDocument.put(UPDATED_AT, System.currentTimeMillis());
        orderDocument.put(CREATED_BY, CUSTOMER);
        orderDocument.put(UPDATED_BY, MERCHANT);
        orderDocument.put(USE_DELIVERY, false);
        orderDocument.put("useRewards", true);
        orderDocument.put("rewardsAmountUsed", BigDecimal.valueOf(5.0));
        orderDocument.put("customerRewardsPointsUsed", BigDecimal.valueOf(500.0));
        return orderDocument;
    }
    private Response getMockedFailedResponse(String message) {
        Response response = new Response();
        response.setStatus(FAILURE);
        response.setMessage(message);
        return response;
    }
    private Response getMockedSuccessResponse(String message, JsonNode data) {
        Response response = new Response();
        response.setStatus(SUCCESS);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}