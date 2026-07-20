package coffee.api.model;

import lombok.Data;
import java.util.UUID;

@Data
public class Drink {
  private UUID drinkId;
  private UUID drinkCategoryId;
  private String categoryName;
  private String drinkName;
  private String size;
  private Double price;
  private String imageUrl;
  private Integer status;
  private Boolean isDeleted;
}
