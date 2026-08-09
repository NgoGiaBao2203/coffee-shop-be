package coffee.api.dto.request.user;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class DeleteStaffRequest {
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID profileId;
}
