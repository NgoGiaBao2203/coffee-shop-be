package coffee.api.dto.request.category;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class EditCategoriesRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID categoryId;

  // Allows: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Size(max = 100, message = ValidationMessage.SIZE_MAX)
  @Pattern(regexp = ValidationPattern.TEXT, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String categoryName;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID shopId;
}
