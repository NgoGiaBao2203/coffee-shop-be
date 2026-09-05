package coffee.api.dto.request.drink;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class EditDrinksRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID drinkId;

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String drinkName;

  // Image URL is optional - if not provided, old image is kept
  private String imageUrl;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private Integer status;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID drinkCategoryId;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private Float price;

  @NotBlank(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String size;
}
