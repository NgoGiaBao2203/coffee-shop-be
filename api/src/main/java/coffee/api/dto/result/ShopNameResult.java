package coffee.api.dto.result;

import lombok.Data;

import java.util.UUID;

@Data
public class ShopNameResult {
  private UUID shopId;
  private String shopName;
}
