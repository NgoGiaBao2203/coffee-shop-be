package coffee.api.dto.request.invoice;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class StatusInvoiceRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID invoiceId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Min(value = 1, message = ValidationMessage.Msg.STATUS_INVALID)
  @Max(value = 2, message = ValidationMessage.Msg.STATUS_INVALID)
  private Integer status;
}
