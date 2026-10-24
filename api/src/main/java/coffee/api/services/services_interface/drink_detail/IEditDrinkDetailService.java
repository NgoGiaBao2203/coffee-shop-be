package coffee.api.services.services_interface.drink_detail;

import coffee.api.dto.request.drink_detail.EditDrinkDetailRequest;
import java.util.UUID;

public interface IEditDrinkDetailService {
  void process(
      EditDrinkDetailRequest request,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId);
}
