package coffee.api.dto.request.shop_branch;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateShopBranchRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String shopName;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String address;

  @Size(max = 11, message = ValidationMessage.Msg.SIZE_MAX)
  @Pattern(regexp = "^[0-9]{10,11}$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String phoneNumber;

  private Boolean isDeleted;
}
