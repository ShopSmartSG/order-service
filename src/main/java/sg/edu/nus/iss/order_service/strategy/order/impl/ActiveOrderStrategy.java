package sg.edu.nus.iss.order_service.strategy.order.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.order_service.db.MongoManager;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.strategy.order.OrderTypeStrategy;
import sg.edu.nus.iss.order_service.utils.Constants;
import sg.edu.nus.iss.order_service.utils.Utils;

import java.util.List;

@Component("ACTIVE")
public class ActiveOrderStrategy extends Constants implements OrderTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ActiveOrderStrategy.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoManager mongoManager;

    @Autowired
    private Utils utils;

    @Value("${" + ORDER_DB + "}")
    private String orderDb;

    @Value("${" + ORDER_COLLECTION + "}")
    private String orderColl;

    @Value("${" + COMPLETED_ORDERS_COLL + "}")
    private String completedOrderColl;

    @Value("${" + CANCELLED_ORDERS_COLL + "}")
    private String cancelledOrderColl;

    private static final String ACTIVE_STATUS = "ACTIVE"; // Filter for active orders

    @Override
    public Response getOrders(String profileType, String profileId) {
        logger.info("Fetching active orders for profileType: {} and profileId: {}", profileType, profileId);

        if (profileType == null || profileId == null) {
            logger.error("Invalid input: profileType or profileId is null");
            return utils.getFailedResponse("Invalid profile type or profile ID.");
        }

        Document query = createProfileQuery(profileType, profileId);
        if (query == null) {
            return utils.getFailedResponse("Invalid profile type: " + profileType);
        }

        query.append("status", ACTIVE_STATUS); // Filter for active orders
        logger.debug("Constructed query: {}", query.toJson());

        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, orderColl);

        if (orders == null || orders.isEmpty()) {
            logger.warn("No active orders found for profileType: {} and profileId: {}", profileType, profileId);
            return utils.getFailedResponse("No active orders found for profileType " + profileType + " and ID " + profileId);
        }

        logger.info("Successfully retrieved {} active orders for profileType: {} and profileId: {}", orders.size(), profileType, profileId);

        return buildSuccessResponse(orders);
    }

    private Document createProfileQuery(String profileType, String profileId) {
        Document query = new Document();

        switch (profileType.toLowerCase()) {
            case "customer":
                query.append("customerId", profileId);
                logger.debug("Querying for customer with profileId: {}", profileId);
                break;
            case "merchant":
                query.append("merchantId", profileId);
                logger.debug("Querying for merchant with profileId: {}", profileId);
                break;
            case "deliverypartner":
                query.append("deliveryPartnerId", profileId);
                logger.debug("Querying for delivery partner with profileId: {}", profileId);
                break;
            default:
                logger.warn("Invalid profile type: {}", profileType);
                return null; // Invalid profile type
        }
        return query;
    }

    private Response buildSuccessResponse(List<Document> orders) {
        try {
            JsonNode ordersJson = objectMapper.valueToTree(orders);
            return utils.getSuccessResponse("Active orders retrieved successfully", ordersJson);
        } catch (Exception e) {
            logger.error("Error while processing orders to JSON: {}", e.getMessage(), e);
            return utils.getFailedResponse("Failed to process the orders.");
        }
    }
}
