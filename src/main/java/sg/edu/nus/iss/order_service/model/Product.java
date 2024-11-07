package sg.edu.nus.iss.order_service.model;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class Product {
    private UUID productId;
    private String productName;
    private Category category;
    private String imageUrl;
    private String productDescription;
    private BigDecimal originalPrice;
    private BigDecimal listingPrice;
    private int availableStock;
    private UUID merchantId;
    private String pincode;
}
