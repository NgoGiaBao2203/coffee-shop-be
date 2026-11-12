package coffee.api.dto.request.shop;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class DeleteShopRequest {
  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID shopId;
}
