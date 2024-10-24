package sg.edu.nus.iss.order_service.service;

import org.springframework.stereotype.Service;
import sg.edu.nus.iss.order_service.model.Cart;
import sg.edu.nus.iss.order_service.model.CartItem;
import sg.edu.nus.iss.order_service.repository.CartRepository;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public Cart getCart(String userId) {
        Optional<Cart> cart = cartRepository.findByUserId(userId);
        return cart.orElseGet(() -> new Cart(userId, new ArrayList<>()));
    }

    public void addToCart(String userId, CartItem cartItem) {
        Cart cart = getCart(userId);
        cart.addItem(cartItem);
        cartRepository.save(cart);  // Save the updated cart to MongoDB
    }

    public void removeFromCart(String userId, String productId) {
        Cart cart = getCart(userId);
        cart.removeItem(productId);
        cartRepository.save(cart);  // Save the updated cart to MongoDB
    }
}
