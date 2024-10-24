package sg.edu.nus.iss.order_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sg.edu.nus.iss.order_service.model.Cart;
import sg.edu.nus.iss.order_service.model.CartItem;
import sg.edu.nus.iss.order_service.repository.CartRepository;

import java.util.ArrayList;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a test cart with userId "user1"
        cart = new Cart("user1", new ArrayList<>());
    }

    @Test
    void addToCart_shouldAddNewItemIfNotPresent() {
        CartItem newItem = new CartItem("prod123", 2);

        // Mocking repository behavior
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));

        // Execute the service method
        cartService.addToCart("user1", newItem);

        // Verify interactions and assert the item was added
        assertEquals(1, cart.getItems().size());
        assertEquals("prod123", cart.getItems().get(0).getProductId());
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void addToCart_shouldIncreaseQuantityIfItemExists() {
        CartItem existingItem = new CartItem("prod123", 2);
        cart.addItem(existingItem);

        CartItem newItem = new CartItem("prod123", 3);

        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));

        // Add the item again
        cartService.addToCart("user1", newItem);

        // Verify the quantity was increased instead of adding a new item
        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void removeFromCart_shouldRemoveItem() {
        CartItem item = new CartItem("prod123", 2);
        cart.addItem(item);

        when(cartRepository.findByUserId("user1")).thenReturn(Optional.of(cart));

        // Execute the remove method
        cartService.removeFromCart("user1", "prod123");

        // Verify the item was removed
        assertEquals(0, cart.getItems().size());
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void getCart_shouldReturnNewCartIfNoneExists() {
        when(cartRepository.findByUserId("user1")).thenReturn(Optional.empty());

        Cart cart = cartService.getCart("user1");

        assertNotNull(cart);
        assertEquals("user1", cart.getUserId());
        assertTrue(cart.getItems().isEmpty());
    }
}
