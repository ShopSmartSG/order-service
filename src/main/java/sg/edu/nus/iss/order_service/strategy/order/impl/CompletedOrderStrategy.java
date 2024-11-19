package sg.edu.nus.iss.order_service.strategy.order.impl;

import com.fasterxml.jackson.databind.JsonNode;
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

@Component("COMPLETED")
public class CompletedOrderStrategy extends Constants implements OrderTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(CompletedOrderStrategy.class);

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

    private static final String COMPLETED_STATUS = "COMPLETED"; // Filter for completed orders

    @Override
    public Response getOrders(String profileType, String profileId) {
        logger.info("Fetching completed orders for profileType: {} and profileId: {}", profileType, profileId);

        Document query = new Document();

        // Build the query based on the profile type
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
                return utils.getFailedResponse("Invalid profile type: " + profileType);
        }

        query.append("status", COMPLETED_STATUS); // Filter for completed orders
        logger.debug("Query built: {}", query.toJson());

        // Query MongoDB for matching documents
        List<Document> orders = mongoManager.findAllDocuments(query, orderDb, completedOrderColl);

        logger.debug("Retrieved {} completed orders", orders.size());

        // Process the results
        if (orders == null || orders.isEmpty()) {
            logger.warn("No completed orders found for profileType: {} and profileId: {}", profileType, profileId);
            return utils.getFailedResponse("No completed orders found for profileType " + profileType + " and ID " + profileId);
        }

        logger.info("Successfully retrieved {} completed orders for profileType: {} and profileId: {}", orders.size(), profileType, profileId);
        return utils.getSuccessResponse("Completed orders retrieved successfully", utils.convertToJsonNode(orders));
    }
}
