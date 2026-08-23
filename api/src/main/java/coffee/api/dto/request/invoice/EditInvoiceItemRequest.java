package coffee.api.dto.request.invoice;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class EditInvoiceItemRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID drinkDetailId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private Integer quantity;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private BigDecimal unitPrice;

  private String note;
}
