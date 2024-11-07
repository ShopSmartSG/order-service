package sg.edu.nus.iss.order_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.order_service.utils.ApplicationConstants;
import sg.edu.nus.iss.order_service.utils.Constants;

@Component
public class OrderDbCollectionResolver extends Constants {

    @Value("${"+ORDER_COLLECTION+"}")
    private String orderCollection;
    @Value("${"+COMPLETED_ORDERS_COLL+"}")
    private String completedOrdersCollection;
    @Value("${"+CANCELLED_ORDERS_COLL+"}")
    private String cancelledOrdersCollection;
    /*
    * Strategy design pattern used here.
    * The OrderCollectionResolver class acts as a strategy for determining
    * the collection name based on the order status
    * Content :- The Order class uses the OrderCollectionResolver to determine
    *              the collection name dynamically
    * Strategy :- The OrderCollectionResolver class encapsulates the logic for
    *               resolving the collection name based on the order status.
    */
    public String resolve(String status) {
        switch (status) {
            case ApplicationConstants.COMPLETED:
                return completedOrdersCollection;
            case ApplicationConstants.CANCELLED:
                return cancelledOrdersCollection;
            default:
                return orderCollection;
        }
    }
}
