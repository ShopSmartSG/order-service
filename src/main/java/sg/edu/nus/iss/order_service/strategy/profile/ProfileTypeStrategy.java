package sg.edu.nus.iss.order_service.strategy.profile;

import com.fasterxml.jackson.databind.JsonNode;
import sg.edu.nus.iss.order_service.model.Response;

public interface ProfileTypeStrategy {
    Response getOrders(String listType, String id);
}
