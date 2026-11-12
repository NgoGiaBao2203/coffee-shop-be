package coffee.api.dto.request.shop;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditShopRequest {

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Size(max = 100, message = ValidationMessage.SIZE_MAX)
  @Pattern(regexp = ValidationPattern.SHOP_NAME, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String shopName;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Size(max = 255, message = ValidationMessage.SIZE_MAX)
  @Pattern(regexp = ValidationPattern.SHOP_ADDRESS, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String address;

  @NotBlank(message = ValidationMessage.FIELD_REQUIRED)
  @Pattern(regexp = ValidationPattern.PHONE_DIGITS, message = ValidationMessage.SPECIAL_CHARACTERS)
  @Pattern(
      regexp = ValidationPattern.PHONE_PREFIX,
      message = ValidationMessage.PHONE_NUMBER_INVALID)
  @Pattern(
      regexp = ValidationPattern.PHONE_LENGTH,
      message = ValidationMessage.PHONE_INVALID_LENGTH)
  private String phoneNumber;
}
