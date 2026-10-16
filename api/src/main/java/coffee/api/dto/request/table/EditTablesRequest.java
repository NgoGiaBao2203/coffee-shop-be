package coffee.api.dto.request.table;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class EditTablesRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID tableId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Min(value = 1, message = ValidationMessage.Msg.PAGE_MIN)
  @Max(value = 9999, message = ValidationMessage.Msg.SIZE_MAX)
  private Integer tableNumber;

  // Allows: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
  @Size(max = 255, message = ValidationMessage.Msg.SIZE_MAX)
  @Pattern(
      regexp = "^\\s*$|^[\\p{L}\\p{N}\\s\\-&/(),.']+$",
      message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String description;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;

  // 1 = Available, 2 = Occupied, 3 = Reserved
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Min(value = 1, message = ValidationMessage.Msg.SIZE_MIN)
  @Max(value = 3, message = ValidationMessage.Msg.SIZE_MAX)
  private Integer status;
}
