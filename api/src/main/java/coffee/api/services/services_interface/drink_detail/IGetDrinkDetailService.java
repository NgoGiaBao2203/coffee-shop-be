package coffee.api.services.services_interface.drink_detail;

import coffee.api.dto.result.DrinkResult;
import java.util.UUID;

public interface IGetDrinkDetailService {
  DrinkResult process(
      UUID drinkId, UUID currentUserId, String currentUserRoleName, UUID currentUserShopId);
}
