package coffee.api.dto.request.drink;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class EditDrinksRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID drinkId;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  // Allows: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
  @Size(max = 100, message = ValidationMessage.SIZE_MAX)
  @Pattern(regexp = ValidationPattern.TEXT, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String drinkName;

  // Image URL is optional - if not provided, old image is kept
  private String imageUrl;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  @Min(value = 0, message = ValidationMessage.STATUS_INVALID)
  @Max(value = 1, message = ValidationMessage.STATUS_INVALID)
  private Integer status;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID drinkCategoryId;
}
