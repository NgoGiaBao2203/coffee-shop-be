package coffee.api.dto.request.invoice;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateInvoiceRequest {

  private UUID shopId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private Integer tableNumber;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @NotEmpty(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Valid
  private List<CreateInvoiceItemRequest> items;
}
