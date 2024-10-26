//package sg.edu.nus.iss.order_service.repository;
//
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.stereotype.Repository;
//import sg.edu.nus.iss.order_service.model.Cart;
//
//import java.util.Optional;
//import java.util.UUID;
//
//@Repository
//public interface CartRepository extends MongoRepository<Cart, UUID> {
//    Optional<Cart> findByCustomerId(UUID customerId);
//    void deleteByCustomerId(UUID customerId);
//    Optional<Cart> findByCustomerIdAndMerchantId(UUID customerId, UUID merchantId);
//}
