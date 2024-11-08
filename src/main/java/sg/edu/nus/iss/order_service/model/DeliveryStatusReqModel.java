package sg.edu.nus.iss.order_service.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class DeliveryStatusReqModel {
    private String orderId;
    private String customerId;
    private String deliveryPersonId;
    private OrderStatus status;
    private String message;
}
