package coffee.api.dto.request.common;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileRequest {
  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  private String username;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Size(min = 8, message = ValidationMessage.PASSWORD_MIN_LENGTH)
  private String password;
}
