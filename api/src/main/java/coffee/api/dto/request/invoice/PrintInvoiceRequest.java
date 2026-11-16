package coffee.api.dto.request.invoice;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class PrintInvoiceRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID invoiceId;
}
