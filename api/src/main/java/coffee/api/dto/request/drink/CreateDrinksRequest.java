package coffee.api.dto.request.drink;

import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateDrinksRequest {
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private String drinkName;
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
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
  private UUID drinkId;
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID shopID;
  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private UUID drinkCategoryId;
}