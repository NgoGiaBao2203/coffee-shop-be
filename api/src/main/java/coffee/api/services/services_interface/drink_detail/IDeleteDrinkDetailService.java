package coffee.api.services.services_interface.drink_detail;

import coffee.api.dto.request.drink_detail.DeleteDrinkDetailRequest;
import java.util.UUID;

public interface IDeleteDrinkDetailService {
  void process(
      DeleteDrinkDetailRequest request,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId);
}
