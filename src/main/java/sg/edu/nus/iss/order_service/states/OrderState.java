package sg.edu.nus.iss.order_service.states;

import sg.edu.nus.iss.order_service.model.OrderContext;
import sg.edu.nus.iss.order_service.model.Response;

public interface OrderState {
    Response updateStatus(OrderContext context);
}
