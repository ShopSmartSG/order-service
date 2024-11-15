package sg.edu.nus.iss.order_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import sg.edu.nus.iss.order_service.db.MongoManager;
import sg.edu.nus.iss.order_service.model.Cart;
import sg.edu.nus.iss.order_service.model.Item;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.utils.Constants;
import sg.edu.nus.iss.order_service.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class CartServiceTest extends Constants {
    private final ObjectMapper objectMapper = Json.mapper();
    @Mock
    private ObjectMapper mapper;
    @Mock
    private MongoManager mongoManager;
    @Mock
    Utils utils;

    @InjectMocks
    private CartService cartService;

    @Value("${"+CART_DB+"}")
    private String cartDb;

    @Value("${"+CART_COLL+"}")
    private String cartColl;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAddItemToCart_EmptyCart_Success(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(null);
        when(mongoManager.insertDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(true);
        when(utils.getSuccessResponse(any(), any())).thenReturn(getMockedSuccessResponse("Item added to cart successfully", objectMapper.createObjectNode()));
        Response response = cartService.addItemToCart("customer1", new Item("item1", 10), "merchant1");
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Item added to cart successfully", response.getMessage());
    }

    @Test
    public void testAddItemToCart_EmptyCart_Failure(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(null);
        when(mongoManager.insertDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(false);
        when(utils.getFailedResponse(any())).thenReturn(getMockedFailedResponse("Failed to add item to cart"));
        Response response = cartService.addItemToCart("customer1", new Item("item1", 10), "merchant1");
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to add item to cart", response.getMessage());
    }

    @Test
    public void testAddItemToCart_ExistingCart_Success(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(getCartDocument());
        when(mongoManager.findOneAndUpdate(any(), any(),  eq(cartDb), eq(cartColl), anyBoolean(), anyBoolean())).thenReturn(new Document());
        when(utils.getSuccessResponse(any(), any())).thenReturn(getMockedSuccessResponse("Item added to cart successfully", objectMapper.createObjectNode()));
        Response response = cartService.addItemToCart("customer1", new Item("item1", 10), "merchant1");
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Item added to cart successfully", response.getMessage());
    }

    @Test
    public void testAddItemToCart_ExistingCart_Failure(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(null);
        when(mongoManager.findOneAndUpdate(any(), any(),  eq(cartDb), eq(cartColl), anyBoolean(), anyBoolean())).thenReturn(null);
        when(utils.getFailedResponse(any())).thenReturn(getMockedFailedResponse("Failed to add item to cart"));
        Response response = cartService.addItemToCart("customer1", new Item("item1", 10), "merchant1");
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to add item to cart", response.getMessage());
    }

    @Test
    public void testRemoveItemFromCart_EmptyCart(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(null);
        when(utils.getFailedResponse(any())).thenReturn(getMockedFailedResponse("Cart not found"));
        Response response = cartService.removeItemFromCart("customer1", new Item("item1", 10));
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Cart not found", response.getMessage());
    }

    @Test
    public void testRemoveItemFromCart_ItemNotInCart(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(getCartDocument());
        when(utils.getFailedResponse(any())).thenReturn(getMockedFailedResponse("Item not in cart"));
        Response response = cartService.removeItemFromCart("customer1", new Item("item3", 10));
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Item not in cart", response.getMessage());
    }

    @Test
    public void testRemoveItemFromCart_EmptiedCart(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(getCartDocument());
        when(mongoManager.deleteDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(true);
        when(utils.getSuccessResponse(any(), any())).thenReturn(getMockedSuccessResponse("Cart deleted/emptied successfully", objectMapper.createObjectNode()));
        Response response = cartService.removeItemFromCart("customer1", new Item("item1", 10));
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Cart deleted/emptied successfully", response.getMessage());
    }

    @Test
    public void testRemoveItemFromCart_Success(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(getCartDocument());
        when(mongoManager.findOneAndUpdate(any(), any(),  eq(cartDb), eq(cartColl), anyBoolean(), anyBoolean())).thenReturn(new Document());
        when(utils.getSuccessResponse(any(), any())).thenReturn(getMockedSuccessResponse("Item removed from cart successfully", objectMapper.createObjectNode()));
        Response response = cartService.removeItemFromCart("customer1", new Item("item1", 5));
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Item removed from cart successfully", response.getMessage());
    }

    @Test
    public void testRemoveItemFromCart_Failure(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(getCartDocument());
        when(mongoManager.findOneAndUpdate(any(), any(),  eq(cartDb), eq(cartColl), anyBoolean(), anyBoolean())).thenReturn(null);
        when(utils.getFailedResponse(any())).thenReturn(getMockedFailedResponse("Failed to remove item from cart"));
        Response response = cartService.removeItemFromCart("customer1", new Item("item1", 5));
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to remove item from cart", response.getMessage());
    }

    @Test
    public void testGetCartByCustomerId_Success(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(getCartDocument());
        Cart cart = cartService.getCartByCustomerId("customer1");
        assertEquals("customer1", cart.getCustomerId());
        assertEquals("merchant1", cart.getMerchantId());
    }

    @Test
    public void testGetCartByCustomerId_Failure() {
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(null);
        Cart cart = cartService.getCartByCustomerId("customer1");
        assertNull(cart);
    }

    @Test
    public void testFindCartByCustomerId_Success() {
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(getCartDocument());
        when(utils.getSuccessResponse(any(), any())).thenReturn(getMockedSuccessResponse("Cart found", objectMapper.valueToTree(getCartDocument())));
        Response response = cartService.findCartByCustomerId("customer1");
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Cart found", response.getMessage());
    }

    @Test
    public void testFindCartByCustomerId_Failure() {
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(null);
        when(utils.getFailedResponse(any())).thenReturn(getMockedFailedResponse("Cart not found"));
        Response response = cartService.findCartByCustomerId("customer1");
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Cart not found", response.getMessage());
    }

    @Test
    public void testDeleteCartByCustomerId_Success(){
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(getCartDocument());
        when(mongoManager.deleteDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(true);
        when(utils.getSuccessResponse(any(), any())).thenReturn(getMockedSuccessResponse("Cart deleted/emptied successfully", objectMapper.createObjectNode()));
        Response response = cartService.removeItemFromCart("customer1", new Item("item1", 10));
        assertEquals(SUCCESS, response.getStatus());
        assertEquals("Cart deleted/emptied successfully", response.getMessage());
    }

    @Test
    public void testDeleteCartByCustomerId_Failure() {
        when(mongoManager.findDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(getCartDocument());
        when(mongoManager.deleteDocument(any(), eq(cartDb), eq(cartColl))).thenReturn(false);
        when(utils.getFailedResponse(any())).thenReturn(getMockedFailedResponse("Failed to delete/empty cart"));
        Response response = cartService.removeItemFromCart("customer1", new Item("item1", 10));
        assertEquals(FAILURE, response.getStatus());
        assertEquals("Failed to delete/empty cart", response.getMessage());
    }

    private Document getCartDocument() {
        List<Item> cartItems = new ArrayList<>();
        cartItems.add(new Item("item1",10));

        Document cartDocument = new Document();
        cartDocument.put(CUSTOMER_ID, "customer1");
        cartDocument.put(MERCHANT_ID, "merchant1");
        cartDocument.put(CREATED_AT, System.currentTimeMillis());
        cartDocument.put(UPDATED_AT, System.currentTimeMillis());
        cartDocument.put(CART_ITEMS, cartItems);
        return cartDocument;
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