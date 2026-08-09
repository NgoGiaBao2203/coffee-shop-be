package coffee.api.services.services_interface.detail;

import coffee.api.dto.result.DrinkResult;
import java.util.UUID;

public interface IGetDrinkDetailService {
  DrinkResult process(UUID drinkId, String currentUserRoleName, UUID currentUserShopId);
}
