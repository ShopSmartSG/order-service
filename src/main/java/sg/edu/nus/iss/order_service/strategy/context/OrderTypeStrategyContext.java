package sg.edu.nus.iss.order_service.strategy.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import sg.edu.nus.iss.order_service.model.Response;
import sg.edu.nus.iss.order_service.strategy.order.OrderTypeStrategy;
import sg.edu.nus.iss.order_service.utils.Utils;

@Component
public class OrderTypeStrategyContext {

    private static final Logger logger = LoggerFactory.getLogger(OrderTypeStrategyContext.class);

    private final OrderTypeStrategy activeOrderStrategy;
    private final OrderTypeStrategy completedOrderStrategy;
    private final OrderTypeStrategy cancelledOrderStrategy;
    private final OrderTypeStrategy allOrderStrategy;
    private final Utils utils;

    @Autowired
    public OrderTypeStrategyContext(
            @Qualifier("ACTIVE") OrderTypeStrategy activeOrderStrategy,
            @Qualifier("COMPLETED") OrderTypeStrategy completedOrderStrategy,
            @Qualifier("CANCELLED") OrderTypeStrategy cancelledOrderStrategy,
            @Qualifier("ALL") OrderTypeStrategy allOrderStrategy,
            Utils utils
    ) {
        this.activeOrderStrategy = activeOrderStrategy;
        this.completedOrderStrategy = completedOrderStrategy;
        this.cancelledOrderStrategy = cancelledOrderStrategy;
        this.allOrderStrategy = allOrderStrategy;
        this.utils = utils;
    }

    public Response getOrders(String listType, String profileType, String profileId) {
        logger.debug("getOrders called with listType: {}, profileType: {}, profileId: {}", listType, profileType, profileId);

        if (listType == null || profileType == null || profileId == null) {
            logger.warn("Null values detected in input parameters");
            return utils.getFailedResponse("Null values detected in input parameters");
        }

        Response response = null;

        switch (listType.toUpperCase()) {
            case "ACTIVE":
                logger.info("Delegating to ACTIVE strategy.");
                response = activeOrderStrategy.getOrders(profileType, profileId);
                break;
            case "COMPLETED":
                logger.info("Delegating to COMPLETED strategy.");
                response = completedOrderStrategy.getOrders(profileType, profileId);
                break;
            case "CANCELLED":
                logger.info("Delegating to CANCELLED strategy.");
                response = cancelledOrderStrategy.getOrders(profileType, profileId);
                break;
            case "ALL":
                logger.info("Delegating to ALL strategy.");
                response = allOrderStrategy.getOrders(profileType, profileId);
                break;
            default:
                logger.warn("Invalid listType: {}. Returning failure response.", listType);
                return utils.getFailedResponse("Invalid listType: " + listType);
        }

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            logger.warn("No orders found for listType: {}", listType);
            return utils.getFailedResponse("No orders found for listType: " + listType);
        }

        logger.debug("Orders retrieved successfully.");
        return utils.getSuccessResponse("Orders retrieved successfully", response.getData());
    }
}
