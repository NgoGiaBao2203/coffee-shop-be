package coffee.api.dto.request.drink;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateDrinksRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID drinkCategoryId;

  // Allows: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Size(max = 100, message = ValidationMessage.Msg.SIZE_MAX)
  @Pattern(
      regexp = "^[\\p{L}\\p{N}\\s\\-&/(),.']+$",
      message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String drinkName;

  private String imageUrl;

  // 1 = Active, 0 = Inactive
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Min(value = 0, message = ValidationMessage.Msg.STATUS_INVALID)
  @Max(value = 1, message = ValidationMessage.Msg.STATUS_INVALID)
  private Integer status;

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Pattern(regexp = "^(S|M|L|XL|XXL)$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String size;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @DecimalMin(value = "0.0", inclusive = false, message = ValidationMessage.Msg.PRICE_MIN)
  @Digits(integer = 16, fraction = 2, message = ValidationMessage.Msg.SIZE_MAX)
  private BigDecimal price;
}
