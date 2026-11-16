package coffee.api.dto.request.table;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateTablesRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  @Min(value = 1, message = ValidationMessage.PAGE_MIN)
  @Max(value = 9999, message = ValidationMessage.SIZE_MAX)
  private Integer tableNumber;

  // Allows: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
  @Size(max = 255, message = ValidationMessage.SIZE_MAX)
  @Pattern(regexp = ValidationPattern.OPTIONAL_TEXT, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String description;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID shopId;

  // 1 = Available, 2 = Occupied, 3 = Reserved
  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  @Min(value = 1, message = ValidationMessage.SIZE_MIN)
  @Max(value = 3, message = ValidationMessage.SIZE_MAX)
  private Integer status = 1;

  public String trimmedDescription() {
    if (description == null || description.isBlank()) return null;
    return description.trim();
  }
}
