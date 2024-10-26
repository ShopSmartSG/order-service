package sg.edu.nus.iss.order_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.order_service.exception.ResourceNotFoundException;
import sg.edu.nus.iss.order_service.model.Order;
import sg.edu.nus.iss.order_service.model.OrderStatus;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.service.OrderService;
import sg.edu.nus.iss.order_service.utils.Constants;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Manage orders in Shopsmart Application")
public class OrderController extends Constants {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final ObjectMapper mapper = Json.mapper();

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Create order from cart for customer")
    public ResponseEntity<String> createOrderFromCart(@PathVariable UUID customerId) {
        log.info("Creating order from cart for customer with ID {}", customerId);
        String createResp = orderService.createOrderFromCart(customerId);
        if(createResp==null){
            log.error("Some exception happened trying to create order from cart for customer with ID {}", customerId);
            throw new ResourceNotFoundException("Some exception happened trying to create order from cart for customer with ID " + customerId);
        } else if(createResp.isEmpty()){
            log.error("Failed to create order for customerId {}", customerId);
            throw new ResourceNotFoundException("Failed to create order for customerId " + customerId);
        }else{
            log.info("Order created from cart for customer with ID {} with response {}", customerId, createResp);
            return ResponseEntity.ok(createResp);
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Retrieve order by orderId")
    public ResponseEntity<?> getOrderByOrderId(@PathVariable UUID orderId) {
        log.info("Retrieving order by orderId {}", orderId);
        Order order = orderService.getOrderByOrderId(orderId);
        if(order==null){
            log.error("Unable to find order with orderId {}", orderId);
            throw new ResourceNotFoundException("Some error occurred while fetching order by orderId " + orderId);
        }else{
            log.info("Order with ID {} found. Order: {}", orderId, order);
            return ResponseEntity.ok(order);
        }
    }

    @GetMapping("/active/customer/{customerId}")
    @Operation(summary = "Retrieve active orders for customer")
    public ResponseEntity<?> getActiveOrdersByCustomerId(@PathVariable UUID customerId) {
        log.info("Retrieving active orders for customer with ID {}", customerId);
        List<Order> activeOrders = orderService.getActiveOrdersByProfileId(customerId, CUSTOMER_ID);
        if(activeOrders==null){
            log.error("Some exception happened trying to get active orders for customer with ID {}", customerId);
            throw new ResourceNotFoundException("Some exception happened trying to get active orders for customer with ID " + customerId);
        } else if (activeOrders.isEmpty()) {
            log.error("No active orders found with customer ID {}", customerId);
            throw new ResourceNotFoundException("No active orders found with customer ID " + customerId);
        }else{
            log.info("Active orders for customer with ID {} found. Order size: {}", customerId, activeOrders.size());
            return ResponseEntity.ok(activeOrders);
        }
    }

    @GetMapping("/active/merchant/{merchantId}")
    @Operation(summary = "Retrieve active orders for merchant")
    public ResponseEntity<?> getActiveOrdersByMerchantId(@PathVariable UUID merchantId) {
        log.info("Retrieving active orders for merchant with ID {}", merchantId);
        List<Order> activeOrders = orderService.getActiveOrdersByProfileId(merchantId, MERCHANT_ID);
        if(activeOrders==null){
            log.error("Some exception happened trying to get active orders for merchant with ID {}", merchantId);
            throw new ResourceNotFoundException("Some exception happened trying to get active orders for merchant with ID " + merchantId);
        } else if (activeOrders.isEmpty()) {
            log.error("No active orders found with merchant ID {}", merchantId);
            throw new ResourceNotFoundException("No active orders found with merchant ID " + merchantId);
        }else{
            log.info("Active orders for merchant with ID {} found. Order size: {}", merchantId, activeOrders.size());
            return ResponseEntity.ok(activeOrders);
        }
    }

    @GetMapping("/cancelled/customer/{customerId}")
    @Operation(summary = "Retrieve cancelled orders for customer")
    public ResponseEntity<?> getCancelledOrdersByCustomerId(@PathVariable UUID customerId) {
        log.info("Retrieving cancelled orders for customer with ID {}", customerId);
        List<Order> cancelledOrders = orderService.getCancelledOrdersByProfileId(customerId, CUSTOMER_ID);
        if(cancelledOrders==null){
            log.error("Some exception happened trying to get cancelled orders for customer with ID {}", customerId);
            throw new ResourceNotFoundException("Some exception happened trying to get cancelled orders for customer with ID " + customerId);
        } else if (cancelledOrders.isEmpty()) {
            log.error("No cancelled orders found with customer ID {}", customerId);
            throw new ResourceNotFoundException("No cancelled orders found with customer ID " + customerId);
        }else{
            log.info("cancelled orders for customer with ID {} found. Order size: {}", customerId, cancelledOrders.size());
            return ResponseEntity.ok(cancelledOrders);
        }
    }

    @GetMapping("/cancelled/merchant/{merchantId}")
    @Operation(summary = "Retrieve cancelled orders for merchant")
    public ResponseEntity<?> getCancelledOrdersByMerchantId(@PathVariable UUID merchantId) {
        log.info("Retrieving cancelled orders for merchant with ID {}", merchantId);
        List<Order> cancelledOrders = orderService.getCancelledOrdersByProfileId(merchantId, MERCHANT_ID);
        if (cancelledOrders == null) {
            log.error("Some exception happened trying to get cancelled orders for merchant with ID {}", merchantId);
            throw new ResourceNotFoundException("Some exception happened trying to get cancelled orders for merchant with ID " + merchantId);
        } else if (cancelledOrders.isEmpty()) {
            log.error("No cancelled orders found with merchant ID {}", merchantId);
            throw new ResourceNotFoundException("No cancelled orders found with merchant ID " + merchantId);
        } else {
            log.info("cancelled orders for merchant with ID {} found. Order size: {}", merchantId, cancelledOrders.size());
            return ResponseEntity.ok(cancelledOrders);
        }
    }

    @GetMapping("/completed/customer/{customerId}")
    @Operation(summary = "Retrieve completed orders for customer")
    public ResponseEntity<?> getCompletedOrdersByCustomerId(@PathVariable UUID customerId) {
        log.info("Retrieving completed orders for customer with ID {}", customerId);
        List<Order> completedOrders = orderService.getCompletedOrdersByProfileId(customerId, CUSTOMER_ID);
        if(completedOrders==null){
            log.error("Some exception happened trying to get completed orders for customer with ID {}", customerId);
            throw new ResourceNotFoundException("Some exception happened trying to get completed orders for customer with ID " + customerId);
        } else if (completedOrders.isEmpty()) {
            log.error("No completed orders found with customer ID {}", customerId);
            throw new ResourceNotFoundException("No completed orders found with customer ID " + customerId);
        }else{
            log.info("completed orders for customer with ID {} found. Order size: {}", customerId, completedOrders.size());
            return ResponseEntity.ok(completedOrders);
        }
    }

    @GetMapping("/completed/merchant/{merchantId}")
    @Operation(summary = "Retrieve completed orders for merchant")
    public ResponseEntity<?> getCompletedOrdersByMerchantId(@PathVariable UUID merchantId) {
        log.info("Retrieving completed orders for merchant with ID {}", merchantId);
        List<Order> completedOrders = orderService.getCompletedOrdersByProfileId(merchantId, MERCHANT_ID);
        if (completedOrders == null) {
            log.error("Some exception happened trying to get completed orders for merchant with ID {}", merchantId);
            throw new ResourceNotFoundException("Some exception happened trying to get completed orders for merchant with ID " + merchantId);
        } else if (completedOrders.isEmpty()) {
            log.error("No completed orders found with merchant ID {}", merchantId);
            throw new ResourceNotFoundException("No completed orders found with merchant ID " + merchantId);
        } else {
            log.info("completed orders for merchant with ID {} found. Order size: {}", merchantId, completedOrders.size());
            return ResponseEntity.ok(completedOrders);
        }
    }

    @GetMapping("/completed/customer/{customerId}")
    @Operation(summary = "Retrieve completed orders for customer")
    public ResponseEntity<?> getAllOrdersByCustomerId(@PathVariable UUID customerId) {
        log.info("Retrieving all orders for customer with ID {}", customerId);
        List<Order> allOrders = orderService.getAllOrdersByProfileId(customerId, CUSTOMER_ID);
        if(allOrders==null){
            log.error("Some exception happened trying to get all orders for customer with ID {}", customerId);
            throw new ResourceNotFoundException("Some exception happened trying to get all orders for customer with ID " + customerId);
        } else if (allOrders.isEmpty()) {
            log.error("No orders found with customer ID {}", customerId);
            throw new ResourceNotFoundException("No orders found with customer ID " + customerId);
        }else{
            log.info("All orders for customer with ID {} found. Order size: {}", customerId, allOrders.size());
            return ResponseEntity.ok(allOrders);
        }
    }

    @GetMapping("/completed/merchant/{merchantId}")
    @Operation(summary = "Retrieve completed orders for merchant")
    public ResponseEntity<?> getAllOrdersByMerchantId(@PathVariable UUID merchantId) {
        log.info("Retrieving all orders for merchant with ID {}", merchantId);
        List<Order> completedOrders = orderService.getAllOrdersByProfileId(merchantId, MERCHANT_ID);
        if (completedOrders == null) {
            log.error("Some exception happened trying to get all orders for merchant with ID {}", merchantId);
            throw new ResourceNotFoundException("Some exception happened trying to get all orders for merchant with ID " + merchantId);
        } else if (completedOrders.isEmpty()) {
            log.error("No orders found with merchant ID {}", merchantId);
            throw new ResourceNotFoundException("No orders found with merchant ID " + merchantId);
        } else {
            log.info("All orders for merchant with ID {} found. Order size: {}", merchantId, completedOrders.size());
            return ResponseEntity.ok(completedOrders);
        }
    }

    @PutMapping("/{orderId}/{status}")
    @Operation(summary = "Update order status by orderId")
    public ResponseEntity<?> updateOrderStatus(@PathVariable UUID orderId, @PathVariable OrderStatus status) {
        log.info("Updating order status by orderId {}", orderId);
        boolean updateResp = orderService.updateOrderStatus(orderId, status);
        if(!updateResp){
            log.error("Failed to update order status {} for orderId {}", status, orderId);
            return ResponseEntity.badRequest().body("Failed to update order status " + status + " for orderId " + orderId);
        }else{
            log.info("Order status updated for orderId {} for status {}", orderId, status);
            return ResponseEntity.ok("Order status updated for orderId " + orderId);
        }
    }

}
