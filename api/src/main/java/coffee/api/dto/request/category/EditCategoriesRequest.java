package coffee.api.dto.request.category;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class EditCategoriesRequest {
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID categoryId;
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Pattern(
    regexp = "^[\\p{L}\\p{N}\\s\\-']+$",
    message = ValidationMessage.Msg.SPECIAL_CHARACTERS
  )
  private String categoryName;
}
