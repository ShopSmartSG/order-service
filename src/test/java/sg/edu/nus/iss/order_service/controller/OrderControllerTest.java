package sg.edu.nus.iss.order_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import sg.edu.nus.iss.order_service.exception.ResourceNotFoundException;
import sg.edu.nus.iss.order_service.model.Order;
import sg.edu.nus.iss.order_service.model.OrderStatus;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.service.OrderService;
import sg.edu.nus.iss.order_service.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class OrderControllerTest extends Constants {
    private final ObjectMapper objectMapper = Json.mapper();
    @Mock
    private ObjectMapper mapper;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateOrderFromCart_Success() {
        when(orderService.createOrderFromCart(anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(getMockedSuccessResponse("Order created", objectMapper.createObjectNode()));
        ResponseEntity<JsonNode> response = orderController.createOrderFromCart("1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Order created", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testCreateOrderFromCart_Failure() {
        when(orderService.createOrderFromCart(anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(getMockedFailedResponse("Failed to create order for customer"));

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.createOrderFromCart("1");
        });
        assertEquals("Failed to create order for customerId 1", exception.getMessage());
    }

    @Test
    public void testCreateOrderFromCart_NullResp() {
        when(orderService.createOrderFromCart(anyString(), anyBoolean(), anyBoolean())).thenReturn(null);

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.createOrderFromCart("1");
        });
        assertEquals("Some exception happened trying to create order from cart for customer with ID 1", exception.getMessage());
    }

    @Test
    public void testCreateOrderFromCartV2_Success() {
        when(orderService.createOrderFromCart(anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(getMockedSuccessResponse("Order created", objectMapper.createObjectNode()));
        ResponseEntity<JsonNode> response = orderController.createOrderFromCartV2("1", true, true);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Order created", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testCreateOrderFromCartV2_Failure() {
        when(orderService.createOrderFromCart(anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(getMockedFailedResponse("Failed to create order for customer"));

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.createOrderFromCartV2("1", true, true);
        });
        assertEquals("Failed to create order for customerId 1", exception.getMessage());
    }

    @Test
    public void testCreateOrderFromCartV2_NullResp() {
        when(orderService.createOrderFromCart(anyString(), anyBoolean(), anyBoolean())).thenReturn(null);

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.createOrderFromCartV2("1", true, true);
        });
        assertEquals("Some exception happened trying to create order from cart for customer with ID 1", exception.getMessage());
    }

    @Test
    public void testGetOrderByOrderId_Success() {
        Order order = new Order();
        order.setOrderId("orderId1");

        when(orderService.getOrderByOrderId(anyString()))
                .thenReturn(getMockedSuccessResponse("Order found", objectMapper.convertValue(order, JsonNode.class)));
        ResponseEntity<JsonNode> response = orderController.getOrderByOrderId("1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(objectMapper.convertValue(order, JsonNode.class), response.getBody());
    }

    @Test
    public void testGetOrderByOrderId_Failure() {
        when(orderService.getOrderByOrderId(anyString())).thenReturn(getMockedFailedResponse("Order not found"));

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.getOrderByOrderId("1");
        });
        assertEquals("Order with ID 1 not found", exception.getMessage());
    }

    @Test
    public void testGetOrderByOrderId_NullResp() {
        when(orderService.getOrderByOrderId(anyString())).thenReturn(null);

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.getOrderByOrderId("1");
        });
        assertEquals("Some error occurred while fetching order by orderId 1", exception.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_Success() {
        List<Order> orderList = new ArrayList<>();
        orderList.add(new Order());

        when(orderService.getOrdersListByProfileId(anyString(), anyString(), anyString()))
                .thenReturn(getMockedSuccessResponse("Orders found", objectMapper.convertValue(orderList, JsonNode.class)));
        ResponseEntity<JsonNode> response = orderController.getOrdersListByProfileId(ACTIVE, CUSTOMER, "1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(objectMapper.convertValue(orderList, JsonNode.class), response.getBody());
    }

    @Test
    public void testGetOrdersListByProfileId_Failure(){
        when(orderService.getOrdersListByProfileId(anyString(), anyString(), anyString()))
                .thenReturn(getMockedFailedResponse("No orders found"));

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.getOrdersListByProfileId(ACTIVE, CUSTOMER, "1");
        });
        assertEquals("No orders found with ID 1", exception.getMessage());
    }

    @Test
    public void testGetOrdersListByProfileId_NullResp(){
        when(orderService.getOrdersListByProfileId(anyString(), anyString(), anyString())).thenReturn(null);

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.getOrdersListByProfileId(ACTIVE, CUSTOMER, "1");
        });
        assertEquals("Some exception happened trying to get orders with ID 1", exception.getMessage());
    }

    @Test
    public void testGetActiveOrdersForDelivery_Success() {
        List<Order> orderList = new ArrayList<>();
        orderList.add(new Order());

        when(orderService.getActiveOrdersForDelivery())
                .thenReturn(getMockedSuccessResponse("Active orders found", objectMapper.convertValue(orderList, JsonNode.class)));
        ResponseEntity<JsonNode> response = orderController.getActiveOrdersForDelivery();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(objectMapper.convertValue(orderList, JsonNode.class), response.getBody());
    }

    @Test
    public void testGetActiveOrdersForDelivery_Failure(){
        when(orderService.getActiveOrdersForDelivery())
                .thenReturn(getMockedFailedResponse("No active orders found"));

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.getActiveOrdersForDelivery();
        });
        assertEquals("No active orders found marked for delivery and in READY state", exception.getMessage());
    }

    @Test
    public void testGetActiveOrdersForDelivery_NullResp(){
        when(orderService.getActiveOrdersForDelivery()).thenReturn(null);

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            orderController.getActiveOrdersForDelivery();
        });
        assertEquals("Some exception happened trying to get all active orders for delivery and in READY state", exception.getMessage());
    }

    @Test
    public void testUpdateOrderStatus_Success(){
        when(orderService.updateOrderStatus(anyString(), any(), any()))
                .thenReturn(getMockedSuccessResponse("Order status updated", objectMapper.createObjectNode()));
        ResponseEntity<JsonNode> response = orderController.updateOrderStatus("order1", OrderStatus.READY, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Order status updated for orderId order1", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testUpdateOrderStatus_Failure(){
        when(orderService.updateOrderStatus(anyString(), any(), any()))
                .thenReturn(getMockedFailedResponse("Failed to update order status"));

        ResponseEntity<JsonNode> response = orderController.updateOrderStatus("order1", OrderStatus.READY, null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to update order status READY for orderId order1", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testUpdateOrderStatus_NullResp(){
        when(orderService.updateOrderStatus(anyString(), any(), any())).thenReturn(null);

        ResponseEntity<JsonNode> response = orderController.updateOrderStatus("order1", OrderStatus.READY, null);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        assertEquals("Some exception happened trying to update order status READY for orderId order1", response.getBody().get(MESSAGE).asText());
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