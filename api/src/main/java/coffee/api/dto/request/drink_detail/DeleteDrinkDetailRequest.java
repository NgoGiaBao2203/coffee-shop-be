package coffee.api.dto.request.drink_detail;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class DeleteDrinkDetailRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID drinkDetailId;
}
