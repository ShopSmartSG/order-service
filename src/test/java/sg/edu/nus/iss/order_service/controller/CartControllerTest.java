package sg.edu.nus.iss.order_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import sg.edu.nus.iss.order_service.model.CartItem;
import sg.edu.nus.iss.order_service.service.CartService;

class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();
    }

    @Test
    void addToCart_shouldReturnSuccessMessage() throws Exception {
        CartItem cartItem = new CartItem("prod123", 2);

        // Perform the POST request
        mockMvc.perform(post("/api/cart/add?userId=user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(cartItem)))
                .andExpect(status().isOk())
                .andExpect(content().string("Item added to cart!"));

        verify(cartService, times(1)).addToCart(eq("user1"), any(CartItem.class));
    }

    @Test
    void removeFromCart_shouldReturnSuccessMessage() throws Exception {

        // Perform the DELETE request
        mockMvc.perform(delete("/api/cart/remove?userId=user1&productId=prod123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Item removed from cart!"));

        verify(cartService, times(1)).removeFromCart("user1", "prod123");
    }
}
