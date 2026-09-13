package coffee.api.dto.request.category;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class EditCategoriesRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID categoryId;

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Size(max = 100, message = ValidationMessage.Msg.SIZE_MAX)
  @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-']+$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String categoryName;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;
}
