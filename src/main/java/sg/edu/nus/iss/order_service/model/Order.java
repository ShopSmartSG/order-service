package sg.edu.nus.iss.order_service.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Order {
    /*
    * When moving to use annotation based mongo updates
    * then use : @Document(collection = "#{@orderCollectionResolver.resolve(#root)}")
    * and for orderId ;
    * @Id
    * @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    */
    private String orderId;
    private String customerId;
    private String merchantId;
    private String deliveryPartnerId;
    private List<Item> orderItems;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private long createdDate;
    private long updatedDate;
    private String createdBy;
    private String updatedBy;
    private boolean useRewards = false;
    private boolean useDelivery = false;
    private BigDecimal rewardsAmountUsed;
    private BigDecimal customerRewardsPointsUsed;
}
