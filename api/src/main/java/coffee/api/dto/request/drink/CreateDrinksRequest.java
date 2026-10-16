package coffee.api.dto.request.drink;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateDrinksRequest {

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopId;

  @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-']+$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String drinkName;

  private String imageUrl;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private Integer status;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private Boolean isDeleted;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private Float price;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String size;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID drinkCategoryId;

  private UUID drinkDetailId;
}
