package coffee.api.dto.result;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class DrinkDetailResult {
  private UUID drinkDetailId;
  private UUID drinkId;
  private String size;
  private BigDecimal price;
}
