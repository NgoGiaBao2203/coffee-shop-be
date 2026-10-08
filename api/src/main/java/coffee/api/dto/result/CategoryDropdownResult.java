package coffee.api.dto.result;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDropdownResult {
  private UUID categoryId;
  private String categoryName;
  private UUID shopId;
  private String shopName;
}
