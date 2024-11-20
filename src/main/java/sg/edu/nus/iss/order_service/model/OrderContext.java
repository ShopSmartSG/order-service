package sg.edu.nus.iss.order_service.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.bson.Document;
import sg.edu.nus.iss.order_service.states.OrderState;

@Data
public class OrderContext {
    private OrderState currentState;
    private Document orderDoc;
    private String orderId;
    private JsonNode payload;


    public Response updateStatus() {
        return currentState.updateStatus(this);
    }
}
