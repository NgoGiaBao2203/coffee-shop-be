package coffee.api.dto.request.user;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateStaffRequest {
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String email;
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String username;
  private String fullName;
  private String phoneNumber;
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID roleId;
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;
}
