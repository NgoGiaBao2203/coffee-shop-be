package coffee.api.dto.request.category;

import java.util.UUID;
import lombok.Data;

@Data
public class DeleteCategoriesRequest {
  private UUID categoryId;
}
