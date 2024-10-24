package sg.edu.nus.iss.order_service.controller;

import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.order_service.HomeController;
import sg.edu.nus.iss.order_service.model.CartItem;
import sg.edu.nus.iss.order_service.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    Logger logger = org.slf4j.LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam String userId, @RequestBody CartItem cartItem) {
        cartService.addToCart(userId, cartItem);
        logger.info("{\"message\": \"Welcome to Shopsmart Order Management"+"\"}");
        return "Item added to cart!";
    }

    @DeleteMapping("/remove")
    public String removeFromCart(@RequestParam String userId, @RequestParam String productId) {
        cartService.removeFromCart(userId, productId);
        logger.info("{\"message\": \"Welcome to Shopsmart Order Management"+"\"}");
        return "Item removed from cart!";
    }
}

