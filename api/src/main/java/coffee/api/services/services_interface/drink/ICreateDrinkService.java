package coffee.api.services.services_interface.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import java.util.UUID;

public interface ICreateDrinkService {
  void process(CreateDrinksRequest request, UUID currentShopID, String roleName);
}
