package coffee.api.dto.request.invoice;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class CancelInvoiceRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID invoiceId;
}
