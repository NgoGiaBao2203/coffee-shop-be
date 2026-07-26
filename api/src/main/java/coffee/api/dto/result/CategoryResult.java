package coffee.api.dto.result;

import lombok.Data;

import java.util.UUID;

@Data
public class CategoryResult {
  private UUID categoryId;
  private UUID shopId;
  private String categoryName;
}
