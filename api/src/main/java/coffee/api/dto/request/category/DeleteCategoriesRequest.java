package coffee.api.dto.request.category;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class DeleteCategoriesRequest {
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID categoryId;
}
