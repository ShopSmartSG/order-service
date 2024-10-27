package sg.edu.nus.iss.order_service.model;

import lombok.Data;

import java.util.UUID;

@Data
public class Category {
    private UUID categoryId;
    private String categoryName;
    private String categoryDescription;
}
