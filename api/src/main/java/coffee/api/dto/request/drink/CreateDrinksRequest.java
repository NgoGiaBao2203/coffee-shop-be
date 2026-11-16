package coffee.api.dto.request.drink;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateDrinksRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID shopId;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID drinkCategoryId;

  // Allows: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Size(max = 100, message = ValidationMessage.SIZE_MAX)
  @Pattern(regexp = ValidationPattern.TEXT, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String drinkName;

  private String imageUrl;

  // 1 = Active, 0 = Inactive
  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  @Min(value = 0, message = ValidationMessage.STATUS_INVALID)
  @Max(value = 1, message = ValidationMessage.STATUS_INVALID)
  private Integer status;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Pattern(regexp = ValidationPattern.DRINK_SIZE, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String size;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  @DecimalMin(value = "0.0", inclusive = false, message = ValidationMessage.PRICE_MIN)
  @Digits(integer = 16, fraction = 2, message = ValidationMessage.SIZE_MAX)
  private BigDecimal price;
}
