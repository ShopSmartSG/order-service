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
import sg.edu.nus.iss.order_service.model.Cart;
import sg.edu.nus.iss.order_service.model.Item;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.service.CartService;
import sg.edu.nus.iss.order_service.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CartControllerTest extends Constants {
    private final ObjectMapper objectMapper = Json.mapper();
    @Mock
    private ObjectMapper mapper;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetCartByCustomerId_Success() {
        List<Item> cartItems = new ArrayList<>();
        cartItems.add(new Item("1", 10));

        Cart cart = new Cart();
        cart.setCartItems(cartItems);
        cart.setCustomerId("1");

        when(cartService.findCartByCustomerId(anyString()))
                .thenReturn(getMockedSuccessResponse("Cart found", objectMapper.convertValue(cart, JsonNode.class)));

        ResponseEntity<JsonNode> response = cartController.getCartByCustomerId("1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(objectMapper.convertValue(cart, JsonNode.class), response.getBody());
    }

    @Test
    public void testGetCartByCustomerId_Failure() {
        when(cartService.findCartByCustomerId(anyString())).thenReturn(getMockedFailedResponse("Cart not found"));

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            cartController.getCartByCustomerId("1");
        });

        assertEquals("Cart not found", exception.getMessage());
    }

    @Test
    public void testGetCartByCustomerId_NullResp() {
        when(cartService.findCartByCustomerId(anyString())).thenReturn(null);

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            cartController.getCartByCustomerId("1");
        });

        assertEquals("Some error occurred while fetching cart for customer with ID 1", exception.getMessage());
    }

    @Test
    public void testAddItemToCart_Success() {
        when(cartService.addItemToCart(anyString(), any(), anyString()))
                .thenReturn(getMockedSuccessResponse("Item added to cart successfully", objectMapper.createObjectNode()));

        Item itemToAdd = new Item("1", 10);
        ResponseEntity<JsonNode> response = cartController.addItemToCart("customer1", itemToAdd, "merchant1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Item added to cart successfully", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testAddItemToCart_Failure() {
        when(cartService.addItemToCart(anyString(), any(), anyString())).thenReturn(getMockedFailedResponse("Failed to add item to cart"));

        Item itemToAdd = new Item("1", 10);
        ResponseEntity<JsonNode> response = cartController.addItemToCart("customer1", itemToAdd, "merchant1");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to add item to cart", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testAddItemToCart_NullResp() {
        when(cartService.addItemToCart(anyString(), any(), anyString())).thenReturn(null);

        Item itemToAdd = new Item("1", 10);
        ResponseEntity<JsonNode> response = cartController.addItemToCart("customer1", itemToAdd, "merchant1");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Some error occurred while trying to add item to cart", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testRemoveItemFromCart_Success() {
        when(cartService.removeItemFromCart(anyString(), any()))
                .thenReturn(getMockedSuccessResponse("Item removed from cart successfully", objectMapper.createObjectNode()));

        Item itemToRemove = new Item("1", 10);
        ResponseEntity<JsonNode> response = cartController.removeItemFromCart("customer1", itemToRemove);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Item removed from cart successfully", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testRemoveItemFromCart_Failure() {
        when(cartService.removeItemFromCart(anyString(), any())).thenReturn(getMockedFailedResponse("Failed to remove item from cart"));

        Item itemToRemove = new Item("1", 10);
        ResponseEntity<JsonNode> response = cartController.removeItemFromCart("customer1", itemToRemove);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to remove item from cart", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testRemoveItemFromCart_NullResp() {
        when(cartService.removeItemFromCart(anyString(), any())).thenReturn(null);

        Item itemToRemove = new Item("1", 10);
        ResponseEntity<JsonNode> response = cartController.removeItemFromCart("customer1", itemToRemove);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Some error occurred while trying to remove item from cart", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testDeleteCart_Success() {
        when(cartService.deleteCartByCustomerId(anyString()))
                .thenReturn(getMockedSuccessResponse("Cart deleted successfully", objectMapper.createObjectNode()));

        ResponseEntity<JsonNode> response = cartController.deleteCart("customer1");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Cart deleted successfully", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testDeleteCart_Failure() {
        when(cartService.deleteCartByCustomerId(anyString())).thenReturn(getMockedFailedResponse("Failed to delete cart"));

        ResponseEntity<JsonNode> response = cartController.deleteCart("customer1");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Failed to delete cart", response.getBody().get(MESSAGE).asText());
    }

    @Test
    public void testDeleteCart_NullResp() {
        when(cartService.deleteCartByCustomerId(anyString())).thenReturn(null);

        ResponseEntity<JsonNode> response = cartController.deleteCart("customer1");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Some error occurred while trying to delete cart", response.getBody().get(MESSAGE).asText());
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