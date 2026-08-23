package coffee.api.dto.request.shop_branch;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class DeleteShopBranchRequest {
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;
}
