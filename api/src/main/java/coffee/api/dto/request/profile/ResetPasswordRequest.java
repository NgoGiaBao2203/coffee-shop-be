package coffee.api.dto.request.profile;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class ResetPasswordRequest {
  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID profileId;
}
