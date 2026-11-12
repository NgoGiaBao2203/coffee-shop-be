package coffee.api.dto.result;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class DrinkDetailPriceResult {
  private UUID drinkDetailId;
  private BigDecimal price;
}
