package coffee.api.dto.request.common;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileRequest {
  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String username;

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Size(min = 8, message = ValidationMessage.Msg.PASSWORD_MIN_LENGTH)
  private String password;
}
