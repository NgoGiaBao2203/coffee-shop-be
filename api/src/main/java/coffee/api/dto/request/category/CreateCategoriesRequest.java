package coffee.api.dto.request.category;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateCategoriesRequest {
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-']+$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String categoryName;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;
}
