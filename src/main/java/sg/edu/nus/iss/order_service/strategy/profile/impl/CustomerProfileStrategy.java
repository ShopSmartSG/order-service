package sg.edu.nus.iss.order_service.strategy.profile.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.strategy.context.OrderTypeStrategyContext;
import sg.edu.nus.iss.order_service.strategy.profile.ProfileTypeStrategy;
import sg.edu.nus.iss.order_service.utils.Constants;
import sg.edu.nus.iss.order_service.utils.Utils;

@Component("customer")
public class CustomerProfileStrategy extends Constants implements ProfileTypeStrategy {

    private static final Logger logger = LoggerFactory.getLogger(CustomerProfileStrategy.class);

    @Autowired
    private OrderTypeStrategyContext orderTypeStrategyContext;

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

    @Override
    public Response getOrders(String listType, String id) {
        logger.info("Fetching orders for customer with ID: {} and listType: {}", id, listType);

        // Retrieve orders for the customer by invoking the OrderTypeStrategyContext
        JsonNode orders = orderTypeStrategyContext.getOrders(listType, "customer", id).getData();

        // Check if the orders are null or empty and respond accordingly
        if (orders == null || orders.isEmpty()) {
            logger.warn("No orders found for customer with ID: {} and listType: {}", id, listType);
            return utils.getFailedResponse("No orders found for customer with ID: " + id + " and listType: " + listType);
        } else {
            logger.info("Successfully fetched {} orders for customer with ID: {}", orders.size(), id);
            return utils.getSuccessResponse("Orders fetched successfully", orders);
        }
    }
}
