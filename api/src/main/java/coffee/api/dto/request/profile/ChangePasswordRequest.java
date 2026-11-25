package coffee.api.dto.request.profile;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  private String currentPassword;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Size(min = 8, message = ValidationMessage.PASSWORD_MIN_LENGTH)
  @Size(max = 72, message = ValidationMessage.SIZE_MAX)
  @Pattern(
      regexp = ValidationPattern.PASSWORD_NO_WHITESPACE,
      message = ValidationMessage.PASSWORD_WHITESPACE)
  @Pattern(
      regexp = ValidationPattern.PASSWORD_ASCII,
      message = ValidationMessage.PASSWORD_NON_ASCII)
  @Pattern(regexp = ValidationPattern.PASSWORD_STRONG, message = ValidationMessage.PASSWORD_WEAK)
  private String newPassword;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Pattern(
      regexp = ValidationPattern.PASSWORD_NO_WHITESPACE,
      message = ValidationMessage.PASSWORD_WHITESPACE)
  @Pattern(
      regexp = ValidationPattern.PASSWORD_ASCII,
      message = ValidationMessage.PASSWORD_NON_ASCII)
  private String confirmPassword;
}
