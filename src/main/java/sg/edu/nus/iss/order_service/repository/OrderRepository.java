//package sg.edu.nus.iss.order_service.repository;
//
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.stereotype.Repository;
//import sg.edu.nus.iss.order_service.model.Order;
//
//import java.util.UUID;
//
//@Repository
//public interface OrderRepository extends MongoRepository<Order, UUID> {
//    Order findByOrderId(UUID orderId);
//    void deleteByOrderId(UUID orderId);
//    Order findByCustomerIdAndMerchantId(UUID customerId, UUID merchantId);
//}
