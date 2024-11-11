package sg.edu.nus.iss.order_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
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
import sg.edu.nus.iss.order_service.utils.Utils;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Manage orders in Shopsmart Application")
public class OrderController extends Constants {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final ObjectMapper mapper = Json.mapper();

    private final OrderService orderService;
    private final Utils utils;

    @Autowired
    public OrderController(OrderService orderService, Utils utils) {
        this.orderService = orderService;
        this.utils = utils;
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Create order from cart for customer")
    public ResponseEntity<JsonNode> createOrderFromCart(@PathVariable String customerId) {
        log.info("Creating order from cart for customer with ID {}", customerId);
        Response createResp = orderService.createOrderFromCart(customerId, false, false);
        if(createResp==null){
            log.error("Some exception happened trying to create order from cart for customer with ID {}", customerId);
            throw new ResourceNotFoundException("Some exception happened trying to create order from cart for customer with ID " + customerId);
        } else if(FAILURE.equalsIgnoreCase(createResp.getStatus())){
            log.error("Failed to create order for customerId {}", customerId);
            throw new ResourceNotFoundException("Failed to create order for customerId " + customerId);
        }else{
            log.info("Order created from cart for customer with ID {} with response {}", customerId, createResp.getData());
            return ResponseEntity.ok(mapper.convertValue(createResp, JsonNode.class));
        }
    }

    @PutMapping("/{customer-id}/rewards/{use-rewards}/delivery/{use-delivery}")
    @Operation(summary = "Create order from cart for customer")
    public ResponseEntity<JsonNode> createOrderFromCartV2(@PathVariable(name = "customer-id") String customerId,
                                                          @PathVariable(name = "use-rewards") boolean useRewards,
                                                          @PathVariable(name = "use-delivery") boolean useDelivery) {
        log.info("V2 :: Creating order from cart for customer with ID {}, if delivery is needed or not {} and if reward points are needed or not {}", customerId, useDelivery, useRewards);
        Response createResp = orderService.createOrderFromCart(customerId, useRewards, useDelivery);
        if(createResp==null){
            log.error("V2 :: Some exception happened trying to create order from cart for customer with ID {}", customerId);
            throw new ResourceNotFoundException("Some exception happened trying to create order from cart for customer with ID " + customerId);
        } else if(FAILURE.equalsIgnoreCase(createResp.getStatus())){
            log.error("V2 :: Failed to create order for customerId {} using useRewardPoints {} and deliveryNeeded {}", customerId, useRewards, useDelivery);
            throw new ResourceNotFoundException("Failed to create order for customerId " + customerId);
        }else{
            log.info("V2 :: Order created from cart for customer with ID {} with response {}", customerId, createResp.getData());
            return ResponseEntity.ok(mapper.convertValue(createResp, JsonNode.class));
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Retrieve order by orderId")
    public ResponseEntity<JsonNode> getOrderByOrderId(@PathVariable String orderId) {
        log.info("Retrieving order by orderId {}", orderId);
        Response orderResp = orderService.getOrderByOrderId(orderId);
        if(orderResp==null){
            log.error("Some exception happened trying to find order with orderId {}", orderId);
            throw new ResourceNotFoundException("Some error occurred while fetching order by orderId " + orderId);
        }else if (FAILURE.equalsIgnoreCase(orderResp.getStatus())){
            log.error("Order with ID {} not found", orderId);
            throw new ResourceNotFoundException("Order with ID " + orderId + " not found");
        } else{
            log.info("Order with ID {} found. Order: {}", orderId, orderResp.getData());
            return ResponseEntity.ok(orderResp.getData());
        }
    }

    @GetMapping("/{listType}/profiles/{profile-type}/id/{profile-id}")
    @Operation(summary = "Retrieve orders list for a profile type based on listing type")
    public ResponseEntity<JsonNode> getOrdersListByProfileId(@PathVariable String listType, @PathVariable("profile-type") String profileType,
                                                             @PathVariable("profile-id") String id) {
        log.info("Retrieving {} orders for profileType {} with ID {}", listType, profileType, id);
        Response ordersListResp = orderService.getOrdersListByProfileId(listType,profileType, id);
        if (ordersListResp == null) {
            log.error("Some exception happened trying to get {} orders for profileType {} with ID {}", listType, profileType, id);
            throw new ResourceNotFoundException("Some exception happened trying to get orders with ID " + id);
        } else if (FAILURE.equalsIgnoreCase(ordersListResp.getStatus())) {
            log.error("No {} orders found with profileType {} and ID {}", listType, profileType, id);
            throw new ResourceNotFoundException("No orders found with ID " + id);
        } else {
            log.info("{} orders for profileType {} with ID {} found. Orders count : {}", listType, profileType, id, ordersListResp.getData().size());
            return ResponseEntity.ok(ordersListResp.getData());
        }
    }

    @GetMapping("/activeOrdersForDelivery")
    @Operation(summary = "Retrieve all active orders marked for delivery")
    public ResponseEntity<JsonNode> getActiveOrdersForDelivery() {
        log.info("Retrieving all active orders for delivery and in READY state");
        Response activeOrdersListResp = orderService.getActiveOrdersForDelivery();
        if (activeOrdersListResp == null) {
            log.error("Some exception happened trying to get all active orders marked for delivery and in READY state");
            throw new ResourceNotFoundException("Some exception happened trying to get all active orders for delivery and in READY state");
        } else if (FAILURE.equalsIgnoreCase(activeOrdersListResp.getStatus())) {
            log.error("No active orders found marked for delivery and in READY state");
            throw new ResourceNotFoundException("No active orders found marked for delivery and in READY state");
        } else {
            log.info("Active orders found marked for delivery and in READY state. Orders count : {}", activeOrdersListResp.getData().size());
            return ResponseEntity.ok(activeOrdersListResp.getData());
        }
    }

    @PutMapping("/{orderId}/{status}")
    @Operation(summary = "Update order status by orderId")
    public ResponseEntity<JsonNode> updateOrderStatus(@PathVariable String orderId, @PathVariable OrderStatus status,
                                                      @RequestBody(required = false) JsonNode requestBody) {
        log.info("Updating order status by orderId {}", orderId);
        ObjectNode response = mapper.createObjectNode();
        log.info("Optional request body {}", requestBody);
        Response updateResp = orderService.updateOrderStatus(orderId, status, requestBody);
        if(updateResp==null){
            log.error("Some exception happened trying to update order status {} for orderId {}", status, orderId);
            response.put(MESSAGE, "Some exception happened trying to update order status " + status + " for orderId " + orderId);
            return ResponseEntity.internalServerError().body(response);
        }else if(FAILURE.equalsIgnoreCase(updateResp.getStatus())){
            log.error("Failed to update order status {} for orderId {}", status, orderId);
            response.put(MESSAGE, "Failed to update order status " + status + " for orderId " + orderId);
            return ResponseEntity.badRequest().body(response);
        }else{
            log.info("Order status updated for orderId {} for status {}", orderId, status);
            response.put(MESSAGE, "Order status updated for orderId " + orderId);
            return ResponseEntity.ok(response);
        }
    }

}
