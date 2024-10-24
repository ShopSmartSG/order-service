package sg.edu.nus.iss.order_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import sg.edu.nus.iss.order_service.model.Cart;
import sg.edu.nus.iss.order_service.config.MongoConfig;

import java.util.Optional;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByUserId(String userId);
}
