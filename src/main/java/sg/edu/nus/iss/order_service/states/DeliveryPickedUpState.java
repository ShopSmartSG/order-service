package sg.edu.nus.iss.order_service.states;

import com.fasterxml.jackson.databind.JsonNode;
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

@Service
public class DeliveryPickedUpState extends Constants implements OrderState{
    private static final Logger log = LoggerFactory.getLogger(CompletedState.class);
    private final ObjectMapper mapper = Json.mapper();

    private final String orderDb;
    private final String orderColl;
    private final MongoManager mongoManager;
    private final Utils utils;

    public DeliveryPickedUpState(MongoManager mongoManager, Utils utils,
                                 @Value("${"+ORDER_DB+"}")String orderDb, @Value("${"+ORDER_COLLECTION+"}")String orderColl) {
        this.mongoManager = mongoManager;
        this.utils = utils;
        this.orderDb = orderDb;
        this.orderColl = orderColl;
    }

    public Response updateStatus(OrderContext orderContext){
        Document orderDoc = orderContext.getOrderDoc();
        String orderId = orderContext.getOrderId();
        Document query = new Document(ORDER_ID, orderId);
        OrderStatus status = OrderStatus.DELIVERY_PICKED_UP;
        if(!orderDoc.containsKey(USE_DELIVERY) || !orderDoc.get(USE_DELIVERY, Boolean.class) || !orderDoc.containsKey(DELIVERY_PARTNER_ID)){
            log.error("Delivery not yet started for the order: {} or not opted for delivery", orderId);
            return utils.getFailedResponse("Delivery has not yet started for this order or not opted for delivery");
        }
        String deliveryPartnerId = orderDoc.get(DELIVERY_PARTNER_ID, String.class);
        boolean deliveryStatusRes = utils.updateDeliveryStatusforOrder(orderDoc, false, deliveryPartnerId, OrderStatus.DELIVERY_PICKED_UP);
        if(!deliveryStatusRes){
            log.error("Failed to update delivery status for orderId: {} for deliveryPartnerId: {} in delivery service", orderId, deliveryPartnerId);
            return utils.getFailedResponse("Failed to update delivery status for order");
        }
        log.info("Delivery record successfully updated in delivery service for orderId: {} and deliveryPartnerId: {}, updating data in order colls.",
                orderId, deliveryPartnerId);
        Document updateDoc = new Document(STATUS, status);
        updateDoc.put(UPDATED_AT, System.currentTimeMillis());
        updateDoc.put(UPDATED_BY, DELIVERY_PARTNER);
        log.info("Updating delivery status for order: {} using query : {}", orderId, mapper.convertValue(query, JsonNode.class));
        Document result = mongoManager.findOneAndUpdate(query, new Document("$set", updateDoc), orderDb, orderColl, false, true);
        if(result != null){
            log.info("Successfully update delivery status for order: {} and kept status : {}", orderId, status);
            return utils.getSuccessResponse("Delivery status has been updated for the order successfully", null);
        }else{
            log.error("Failed to update delivery status for order: {},  status : {}", orderId, status);
            return utils.getFailedResponse("Failed to update delivery status for order");
        }
    }
}
