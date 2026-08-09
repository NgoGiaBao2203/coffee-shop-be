package coffee.api.services.services_interface.drink;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.DrinkResult;
import java.util.UUID;

public interface IGetDrinkService {
  PageResponse<DrinkResult> process(
      SearchDrinksRequest request, String currentUserRoleName, UUID currentUserShopId);
}
