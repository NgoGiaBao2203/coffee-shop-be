package coffee.api.dto.request.shop;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateShopRequest {

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Size(max = 100, message = ValidationMessage.Msg.SIZE_MAX)
  @Pattern(regexp = "^[\\p{L}0-9\\s.,&'-]+$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String shopName;

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Size(max = 255, message = ValidationMessage.Msg.SIZE_MAX)
  @Pattern(regexp = "^[\\p{L}0-9\\s/.,#-]+$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String address;

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Pattern(regexp = "^\\s*$|^[0-9]+$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  @Pattern(regexp = "^\\s*$|^0.*$", message = ValidationMessage.Msg.PHONE_NUMBER_INVALID)
  @Pattern(regexp = "^\\s*$|^.{10,11}$", message = ValidationMessage.Msg.PHONE_INVALID_LENGTH)
  private String phoneNumber;
}
