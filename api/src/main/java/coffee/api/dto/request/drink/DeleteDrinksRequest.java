package coffee.api.dto.request.drink;

import coffee.api.constants.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class DeleteDrinksRequest {

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private UUID drinkId;
}
