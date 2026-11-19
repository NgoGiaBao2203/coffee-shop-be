package coffee.api.dto.request.staff;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class EditStaffRequest {
  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID profileId;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Size(max = 100, message = ValidationMessage.SIZE_MAX)
  @Pattern(regexp = ValidationPattern.TEXT, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String fullName;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Pattern(regexp = ValidationPattern.PHONE_DIGITS, message = ValidationMessage.SPECIAL_CHARACTERS)
  @Pattern(
      regexp = ValidationPattern.PHONE_PREFIX,
      message = ValidationMessage.PHONE_NUMBER_INVALID)
  @Pattern(
      regexp = ValidationPattern.PHONE_LENGTH,
      message = ValidationMessage.PHONE_INVALID_LENGTH)
  private String phoneNumber;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID roleId;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID shopId;
}
