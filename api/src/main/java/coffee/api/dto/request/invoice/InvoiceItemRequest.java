package coffee.api.dto.request.invoice;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class InvoiceItemRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID drinkDetailId;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  @Min(value = 1, message = ValidationMessage.SIZE_MIN)
  @Max(value = 100, message = ValidationMessage.SIZE_MAX)
  private Integer quantity;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  @DecimalMin(value = "0.0", inclusive = false, message = ValidationMessage.PRICE_MIN)
  @Digits(integer = 16, fraction = 2, message = ValidationMessage.SIZE_MAX)
  private BigDecimal unitPrice;

  @Size(max = 255, message = ValidationMessage.SIZE_MAX)
  private String note;
}
