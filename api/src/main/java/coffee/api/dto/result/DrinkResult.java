package coffee.api.dto.result;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class DrinkResult {
  private UUID drinkId;
  private UUID drinkCategoryId;
  private String drinkName;
  private String imageUrl;
  private String status;
  private Boolean isDeleted;

  private List<DrinkVariantResult> variants;

  @Data
  public static class DrinkVariantResult {
    private String size;
    private BigDecimal price;
  }
}