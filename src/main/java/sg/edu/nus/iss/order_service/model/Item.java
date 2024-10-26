package sg.edu.nus.iss.order_service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item {
    private UUID productId;
    private int quantity;
    private BigDecimal price;

    public Item(UUID productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }
}
