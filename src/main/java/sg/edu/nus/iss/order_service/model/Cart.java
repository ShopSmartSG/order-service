package sg.edu.nus.iss.order_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Data
//@Document(collection = "cart")
public class Cart {
//    @Id
//    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String customerId;
    private List<Item> cartItems;
    private String merchantId;
    private long createdAt;
    private long updatedAt;
}
