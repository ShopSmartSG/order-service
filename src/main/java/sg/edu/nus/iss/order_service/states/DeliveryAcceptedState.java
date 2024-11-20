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
public class DeliveryAcceptedState extends Constants implements OrderState{
    private static final Logger log = LoggerFactory.getLogger(DeliveryAcceptedState.class);
    private final ObjectMapper mapper = Json.mapper();

    private final String orderDb;
    private final String orderColl;
    private final MongoManager mongoManager;
    private final Utils utils;

    public DeliveryAcceptedState(MongoManager mongoManager, Utils utils,
                                 @Value("${"+ORDER_DB+"}")String orderDb, @Value("${"+ORDER_COLLECTION+"}")String orderColl) {
        this.mongoManager = mongoManager;
        this.utils = utils;
        this.orderDb = orderDb;
        this.orderColl = orderColl;
    }

    @Override
    public Response updateStatus(OrderContext orderContext){
        Document orderDoc = orderContext.getOrderDoc();
        String orderId = orderContext.getOrderId();
        Document query = new Document(ORDER_ID, orderId);
        OrderStatus status = OrderStatus.DELIVERY_ACCEPTED;
        if(!orderDoc.containsKey(USE_DELIVERY) || !orderDoc.get(USE_DELIVERY, Boolean.class)){
            log.error("Attempting to start delivery for orderId: {} without opting for delivery", orderId);
            return utils.getFailedResponse("Attempting to start delivery for order which didnt opt for delivery");
        }
        JsonNode payload = orderContext.getPayload();
        if(payload==null || !payload.hasNonNull(DELIVERY_PARTNER_ID)){
            log.error("No delivery partner id found in payload {} for orderId: {} while starting delivery", payload, orderId);
            return utils.getFailedResponse("No delivery partner id provided, unable to start delivery");
        }
        String deliveryPartnerId = payload.get(DELIVERY_PARTNER_ID).asText();
        boolean deliveryStatusRes = utils.updateDeliveryStatusforOrder(orderDoc, true, deliveryPartnerId, OrderStatus.DELIVERY_ACCEPTED);
        if(!deliveryStatusRes){
            log.error("Failed to start delivery for orderId: {} for deliveryPartnerId: {}", orderId, deliveryPartnerId);
            return utils.getFailedResponse("Failed to start delivery for order");
        }
        log.info("Delivery record successfully created in delivery service for orderId: {} and deliveryPartnerId: {}, updating data in order colls.",
                orderId, deliveryPartnerId);
        Document updateDoc = new Document(STATUS, status);
        updateDoc.put(DELIVERY_PARTNER_ID, deliveryPartnerId);
        updateDoc.put(UPDATED_AT, System.currentTimeMillis());
        updateDoc.put(UPDATED_BY, DELIVERY_PARTNER);
        log.info("Starting delivery for order: {} using query : {}", orderId, mapper.convertValue(query, JsonNode.class));
        Document result = mongoManager.findOneAndUpdate(query, new Document("$set", updateDoc), orderDb, orderColl, false, true);
        if(result != null){
            log.info("Started delivery flow successfully for order: {} and kept status : {}", orderId, status);
            return utils.getSuccessResponse("Delivery has been started for the order successfully", null);
        }else{
            log.error("Failed to initiate delivery for order: {},  status : {}", orderId, status);
            return utils.getFailedResponse("Failed to initiate delivery for order");
        }
    }
}
