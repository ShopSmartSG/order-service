package sg.edu.nus.iss.order_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private BigDecimal rewardsAmount;
}
