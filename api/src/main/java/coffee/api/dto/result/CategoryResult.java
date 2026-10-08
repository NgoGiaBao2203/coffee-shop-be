package coffee.api.dto.result;

import java.util.UUID;
import lombok.Data;

@Data
public class CategoryResult {
  private UUID categoryId;
  private UUID shopId;
  private String shopName;
  private String categoryName;
}
