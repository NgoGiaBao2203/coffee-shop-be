package coffee.api.dto.request.table;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class EditTablesRequest {
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID tableId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private Integer tableNumber;

  private String description;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;

  private Integer status; // 1 = available, 2 = occupied, 3 = reserved
}
