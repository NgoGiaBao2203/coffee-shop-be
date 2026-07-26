package coffee.api.services.services_interface.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;

public interface IEditDrinkService {
  void process(EditDrinksRequest request, String currentUserRoleName);
}
