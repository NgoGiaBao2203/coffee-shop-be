package coffee.api.dto.result;

import java.util.UUID;
import lombok.Data;

@Data
public class ShopNameResult {
  private UUID shopId;
  private String shopName;
}
