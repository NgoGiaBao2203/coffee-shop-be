package coffee.api.services.services_interface.drink;

import coffee.api.dto.request.drink.DeleteDrinksRequest;
import java.util.UUID;

public interface IDeleteDrinkService {
  void process(DeleteDrinksRequest request, String currentUserRoleName, UUID currentUserShopId);
}
