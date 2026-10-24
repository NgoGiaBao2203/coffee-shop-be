package coffee.api.services.services_interface.drink_detail;

import coffee.api.dto.request.drink_detail.CreateDrinkDetailRequest;
import java.util.UUID;

public interface ICreateDrinkDetailService {
  void process(
      CreateDrinkDetailRequest request,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId);
}
