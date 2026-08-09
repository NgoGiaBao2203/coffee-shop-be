package coffee.api.dto.request.drink;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class DeleteDrinksRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID drinkId;
}
