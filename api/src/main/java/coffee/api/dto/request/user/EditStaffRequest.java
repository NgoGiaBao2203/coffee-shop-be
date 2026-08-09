package coffee.api.dto.request.user;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class EditStaffRequest {
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID profileId;

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String fullName;

  private String phoneNumber;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID roleId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;
}
