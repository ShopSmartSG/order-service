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

import java.util.ArrayList;
import java.util.List;

@Component("ALL")
public class AllOrderStrategy extends Constants implements OrderTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AllOrderStrategy.class);

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

    private static final String ALL_STATUS = "ALL"; // Filter for all orders

    @Override
    public Response getOrders(String profileType, String profileId) {
        logger.info("Fetching all orders for profileType: {} and profileId: {}", profileType, profileId);

        if (profileType == null || profileId == null) {
            logger.error("Invalid input: profileType or profileId is null");
            return utils.getFailedResponse("Invalid profile type or profile ID.");
        }

        Document query = createProfileQuery(profileType, profileId);
        if (query == null) {
            return utils.getFailedResponse("Invalid profile type: " + profileType);
        }

        query.append("status", ALL_STATUS); // Filter for all orders
        logger.debug("Constructed query: {}", query.toJson());

        // Query MongoDB for matching documents across all collections
        List<Document> allOrders = new ArrayList<>();
        allOrders.addAll(mongoManager.findAllDocuments(query, orderDb, completedOrderColl));
        allOrders.addAll(mongoManager.findAllDocuments(query, orderDb, cancelledOrderColl));
        allOrders.addAll(mongoManager.findAllDocuments(query, orderDb, orderColl));

        // Check if orders were found
        if (allOrders.isEmpty()) {
            logger.warn("No all orders found for profileType: {} and profileId: {}", profileType, profileId);
            return utils.getFailedResponse("No all orders found for profileType " + profileType + " and ID " + profileId);
        }

        // Convert the list of orders to JsonNode
        JsonNode ordersJsonNode = utils.convertToJsonNode(allOrders);

        logger.info("Successfully retrieved {} orders for profileType: {} and profileId: {}", allOrders.size(), profileType, profileId);
        return utils.getSuccessResponse("All orders retrieved successfully", ordersJsonNode);
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
}
