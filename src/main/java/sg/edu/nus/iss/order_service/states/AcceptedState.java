package sg.edu.nus.iss.order_service.states;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.order_service.db.MongoManager;
import sg.edu.nus.iss.order_service.model.OrderContext;
import sg.edu.nus.iss.order_service.model.OrderStatus;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.utils.Constants;
import sg.edu.nus.iss.order_service.utils.Utils;

@Service
public class AcceptedState extends Constants implements OrderState {
    private static final Logger log = LoggerFactory.getLogger(AcceptedState.class);

    private final String orderDb;
    private final String orderColl;
    private final MongoManager mongoManager;
    private final Utils utils;

    public AcceptedState(MongoManager mongoManager, Utils utils,
                         @Value("${"+ORDER_DB+"}")String orderDb, @Value("${"+ORDER_COLLECTION+"}")String orderColl) {
        this.mongoManager = mongoManager;
        this.utils = utils;
        this.orderDb = orderDb;
        this.orderColl = orderColl;
    }

    @Override
    public Response updateStatus(OrderContext orderContext) {
        String orderId = orderContext.getOrderId();
        OrderStatus status = OrderStatus.ACCEPTED;
        Document query = new Document(ORDER_ID, orderId);
        Document updateDoc = new Document(STATUS, status);
        updateDoc.put(UPDATED_AT, System.currentTimeMillis());
        updateDoc.put(UPDATED_BY, MERCHANT);
        Document result = mongoManager.findOneAndUpdate(query, new Document("$set", updateDoc), orderDb, orderColl, false, true);
        if(result != null){
            log.info("Order status updated successfully for orderId: {} to status : {}", orderId, status);
            return utils.getSuccessResponse("Order status updated successfully for orderId: ".concat(orderId).concat(" to status : ").concat(status.toString()), null);
        }else{
            log.error("Failed to update order status for orderId: {} to status : {}", orderId, status);
            return utils.getFailedResponse("Failed to update order status for orderId: ".concat(orderId).concat(" to status : ").concat(status.toString()));
        }
    }
}
