package coffee.api.services.services_interface.drink;

import coffee.api.dto.request.drink.DeleteDrinksRequest;

public interface IDeleteDrinkService {
  void process(DeleteDrinksRequest request);
}
