package coffee.api.dto.request.category;

import lombok.Data;

import java.util.UUID;

@Data
public class DeleteCategoriesRequest {
  private UUID categoryId;
}
