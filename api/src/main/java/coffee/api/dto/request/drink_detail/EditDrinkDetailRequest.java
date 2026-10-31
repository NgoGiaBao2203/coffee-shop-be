package coffee.api.dto.request.drink_detail;

import coffee.api.enums.ValidationMessage;
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

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID drinkDetailId;

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Pattern(regexp = "^[SML]$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String size;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @DecimalMin(value = "0.0", inclusive = false, message = ValidationMessage.Msg.PRICE_MIN)
  @Digits(integer = 16, fraction = 2, message = ValidationMessage.Msg.SIZE_MAX)
  private BigDecimal price;
}
