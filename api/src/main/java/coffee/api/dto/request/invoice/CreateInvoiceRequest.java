package coffee.api.dto.request.invoice;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateInvoiceRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID tableId;

  // 0 = pay later, 1 = paid at the counter.
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Min(value = 0, message = ValidationMessage.Msg.STATUS_INVALID)
  @Max(value = 1, message = ValidationMessage.Msg.STATUS_INVALID)
  private Integer status = 0;

  @NotEmpty(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Valid
  private List<@NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED) InvoiceItemRequest> items;
}
