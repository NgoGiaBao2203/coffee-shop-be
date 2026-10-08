package coffee.api.dto.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class DrinkResult {
  private UUID drinkId;
  private UUID shopId;
  private UUID drinkCategoryId;
  private String categoryName;
  private String drinkName;
  private String imageUrl;
  private String status;
  private Boolean isDeleted;

  private List<DrinkVariantResult> variants;

  @Data
  public static class DrinkVariantResult {
    private UUID drinkDetailId;
    private String size;
    private BigDecimal price;
  }
}
