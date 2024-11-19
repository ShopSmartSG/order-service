package sg.edu.nus.iss.order_service.strategy.order;

import com.fasterxml.jackson.databind.JsonNode;
import sg.edu.nus.iss.order_service.model.Response;

public interface OrderTypeStrategy {
    Response getOrders(String profileType, String profileId);
}
