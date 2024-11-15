package sg.edu.nus.iss.order_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.order_service.exception.ResourceNotFoundException;
import sg.edu.nus.iss.order_service.model.Cart;
import sg.edu.nus.iss.order_service.model.Item;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.service.CartService;
import sg.edu.nus.iss.order_service.utils.Constants;

import java.util.UUID;

@RestController
@RequestMapping("/carts")
@Tag(name = "Carts", description = "Manage carts in Shopsmart Application")
public class CartController extends Constants {
    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    private final ObjectMapper mapper = Json.mapper();

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService){
        this.cartService = cartService;
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Retrieve cart for customer")
    public ResponseEntity<JsonNode> getCartByCustomerId(@PathVariable String customerId) {
        log.info("Retrieving cart for customer with ID {}", customerId);
        Response response = cartService.findCartByCustomerId(customerId);
        if(response==null){
            log.error("Some exception happened to get cart for customer with ID {}", customerId);
            throw new ResourceNotFoundException("Some error occurred while fetching cart for customer with ID " + customerId);
        } else if (FAILURE.equalsIgnoreCase(response.getStatus())){
            log.error("Cart for customer with ID {} not found", customerId);
            throw new ResourceNotFoundException(response.getMessage());
        }else{
            log.info("Cart for customer with ID {} found. Cart: {}", customerId, response.getData());
            return ResponseEntity.ok(mapper.convertValue(response.getData(), JsonNode.class));
        }
    }

    @PutMapping("/add/{customerId}/merchant/{merchantId}")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<JsonNode> addItemToCart(@PathVariable String customerId, @RequestBody Item item, @PathVariable String merchantId) {
        log.info("Adding item {} to cart for customer with ID {}", item, customerId);
        ObjectNode response = mapper.createObjectNode();
        Response result = cartService.addItemToCart(customerId, item, merchantId);
        if(result == null){
            log.info("Some error occurred while trying to add item added to cart for customer with ID {}", customerId);
            response.put(MESSAGE, "Some error occurred while trying to add item to cart");
            return ResponseEntity.internalServerError().body(response);
        } else if(SUCCESS.equalsIgnoreCase(result.getStatus())){
            log.info("Item added to cart for customer with ID {}", customerId);
            response.put(MESSAGE, result.getMessage());
            return ResponseEntity.ok(response);
        }else{
            log.error("Failed to add item to cart for customer with ID {}", customerId);
            response.put(MESSAGE, "Failed to add item to cart");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/remove/{customerId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<JsonNode> removeItemFromCart(@PathVariable String customerId, @RequestBody Item item) {
        log.info("Removing item {} from cart for customer with ID {}", item, customerId);
        ObjectNode response = mapper.createObjectNode();
        Response result = cartService.removeItemFromCart(customerId, item);
        if(result == null){
            log.info("Some error occurred while trying to remove item from cart for customer with ID {}", customerId);
            response.put(MESSAGE, "Some error occurred while trying to remove item from cart");
            return ResponseEntity.internalServerError().body(response);
        } else if(SUCCESS.equalsIgnoreCase(result.getStatus())){
            log.info("Item removed from cart for customer with ID {}", customerId);
            response.put(MESSAGE, result.getMessage());
            return ResponseEntity.ok(response);
        }else{
            log.error("Failed to remove item from cart for customer with ID {}", customerId);
            response.put(MESSAGE, "Failed to remove item from cart");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete or empty cart for customer")
    public ResponseEntity<JsonNode> deleteCart(@PathVariable String customerId) {
        log.info("Deleting or emptying cart for customer with ID {}", customerId);
        ObjectNode response = mapper.createObjectNode();
        Response result = cartService.deleteCartByCustomerId(customerId);
        if(result == null){
            log.info("Some error occurred while trying to delete or empty cart for customer with ID {}", customerId);
            response.put(MESSAGE, "Some error occurred while trying to delete cart");
            return ResponseEntity.internalServerError().body(response);
        } else if(SUCCESS.equalsIgnoreCase(result.getStatus())){
            log.info("Cart deleted/emptied for customer with ID {}", customerId);
            response.put(MESSAGE, result.getMessage());
            return ResponseEntity.ok(response);
        }else{
            log.error("Failed to delete cart for customer with ID {}", customerId);
            response.put(MESSAGE, "Failed to delete cart");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
