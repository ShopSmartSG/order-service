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
public class CompletedState extends Constants implements OrderState{
    private static final Logger log = LoggerFactory.getLogger(CompletedState.class);
    private final ObjectMapper mapper = Json.mapper();

    private final String orderDb;
    private final String orderColl;
    private final String completedOrderColl;
    private final MongoManager mongoManager;
    private final Utils utils;

    public CompletedState(MongoManager mongoManager, Utils utils,
                          @Value("${"+ORDER_DB+"}")String orderDb, @Value("${"+ORDER_COLLECTION+"}")String orderColl,
                          @Value("${"+COMPLETED_ORDERS_COLL+"}")String completedOrderColl) {
        this.mongoManager = mongoManager;
        this.utils = utils;
        this.orderDb = orderDb;
        this.orderColl = orderColl;
        this.completedOrderColl = completedOrderColl;
    }

    @Override
    public Response updateStatus(OrderContext orderContext){
        Document orderDoc = orderContext.getOrderDoc();
        String orderId = orderContext.getOrderId();
        Document query = new Document(ORDER_ID, orderId);
        try{
            boolean usingDelivery = orderDoc.get(USE_DELIVERY, Boolean.class);
            if(usingDelivery && orderDoc.containsKey(DELIVERY_PARTNER_ID)){
                //update delivery service about status change if delivery opted for
                String deliveryPartnerId = orderDoc.get(DELIVERY_PARTNER_ID, String.class);
                boolean deliveryStatusRes = utils.updateDeliveryStatusforOrder(orderDoc, false,
                        deliveryPartnerId, OrderStatus.COMPLETED);
                if(!deliveryStatusRes){
                    log.error("Failed to complete delivery for orderId: {} for deliveryPartnerId: {}", orderId, deliveryPartnerId);
                    return utils.getFailedResponse("Failed to complete delivery for order");
                }
                log.info("Delivery completed successfully in delivery service for orderId: {} and deliveryPartnerId: {}, updating data in order colls.",
                        orderId, deliveryPartnerId);
            }
            orderDoc.put(STATUS, OrderStatus.COMPLETED);
            orderDoc.put(UPDATED_AT, System.currentTimeMillis());
            orderDoc.put(UPDATED_BY, usingDelivery ? DELIVERY_PARTNER : MERCHANT);
            mongoManager.insertDocument(orderDoc, orderDb, completedOrderColl);
            mongoManager.deleteDocument(query, orderDb, orderColl);

            BigDecimal orderPrice =  mapper.convertValue(orderDoc.get(TOTAL_PRICE), BigDecimal.class);
            utils.updateMerchantEarnings(orderId, orderDoc.get(MERCHANT_ID, String.class),  orderPrice);
            utils.updateCustomerRewardPoints(orderId, orderDoc.get(CUSTOMER_ID, String.class), orderPrice);
            log.info("Order has been completed successfully for orderId: {} and kept in completed coll", orderId);
            return utils.getSuccessResponse("Order has been completed successfully for orderId: ".concat(orderId).concat(" and kept in completed coll"), null);
        }catch(Exception ex){
            log.error("Exception occurred while marking order status for orderId: {} as completed", orderId);
            return utils.getFailedResponse("Exception occurred while marking order status for orderId: ".concat(orderId).concat(" as completed "));
        }
    }
}
