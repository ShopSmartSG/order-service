package sg.edu.nus.iss.order_service.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductUpdateReqModel {
    private UUID productId;
    private String productName;
    private UUID categoryId;
    private String imageUrl;
    private String productDescription;
    private BigDecimal originalPrice;
    private BigDecimal listingPrice;
    private int availableStock;
    private String pincode;
    private UUID merchantId;
}
