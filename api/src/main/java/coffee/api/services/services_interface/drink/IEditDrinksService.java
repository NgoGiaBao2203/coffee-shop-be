package coffee.api.services.services_interface.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import java.util.UUID;

public interface IEditDrinksService {
  void process(EditDrinksRequest request, String currentUserRoleName, UUID currentUserShopId);
}
