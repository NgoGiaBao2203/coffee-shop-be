package coffee.api.dto.request.user;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class EditStaffRequest {
  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID profileId;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  private String fullName;

  private String phoneNumber;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID roleId;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID shopId;
}
