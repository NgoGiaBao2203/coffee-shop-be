package coffee.api.dto.request.staff;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class DeleteStaffRequest {
  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID profileId;
}
