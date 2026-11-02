package coffee.api.dto.request.drink_detail;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class EditDrinkDetailRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID drinkDetailId;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Pattern(regexp = ValidationPattern.DRINK_SIZE, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String size;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  @DecimalMin(value = "0.0", inclusive = false, message = ValidationMessage.PRICE_MIN)
  @Digits(integer = 16, fraction = 2, message = ValidationMessage.SIZE_MAX)
  private BigDecimal price;
}
