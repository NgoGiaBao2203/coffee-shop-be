package coffee.api.dto.request.table;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class OccupyTablesRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID tableId;
}
