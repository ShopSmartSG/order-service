package sg.edu.nus.iss.order_service.states;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
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

import java.math.BigDecimal;

@Service
public class CancelledState extends Constants implements OrderState{
    private static final Logger log = LoggerFactory.getLogger(CancelledState.class);
    private final ObjectMapper mapper = Json.mapper();

    private final String orderDb;
    private final String orderColl;
    private final String cancelledOrderColl;
    private final MongoManager mongoManager;
    private final Utils utils;

    public CancelledState(MongoManager mongoManager, Utils utils,
                          @Value("${"+ORDER_DB+"}")String orderDb, @Value("${"+ORDER_COLLECTION+"}")String orderColl,
                          @Value("${"+CANCELLED_ORDERS_COLL+"}")String cancelledOrderColl) {
        this.mongoManager = mongoManager;
        this.utils = utils;
        this.orderDb = orderDb;
        this.orderColl = orderColl;
        this.cancelledOrderColl = cancelledOrderColl;
    }

    @Override
    public Response updateStatus(OrderContext orderContext){
        Document orderDoc = orderContext.getOrderDoc();
        String orderId = orderContext.getOrderId();
        Document query = new Document(ORDER_ID, orderId);

        try{
            orderDoc.put(STATUS, OrderStatus.CANCELLED);
            orderDoc.put(UPDATED_AT, System.currentTimeMillis());
            orderDoc.put(UPDATED_BY, MERCHANT);
            mongoManager.insertDocument(orderDoc, orderDb, cancelledOrderColl);
            mongoManager.deleteDocument(query, orderDb, orderColl);
            //this will restore the reward points for user.
            utils.updateCustomerRewardPoints(orderId, orderDoc.get(CUSTOMER_ID, String.class),
                    mapper.convertValue(orderDoc.get("customerRewardsPointsUsed"), BigDecimal.class));
            log.info("Order has been cancelled successfully for orderId: {} and kept in cancelled coll", orderId);
            return utils.getSuccessResponse("Order has been cancelled successfully for orderId: ".concat(orderId).concat(" and kept in cancelled coll"), null);
        }catch(Exception ex){
            log.error("Exception occurred while marking order status for orderId: {} as cancelled", orderId);
            return utils.getFailedResponse("Exception occurred while marking order status for orderId: ".concat(orderId).concat(" as cancelled "));
        }
    }
}
