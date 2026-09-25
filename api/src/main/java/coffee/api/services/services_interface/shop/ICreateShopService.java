package coffee.api.services.services_interface.shop;

import coffee.api.dto.request.shop.CreateShopRequest;
import java.util.UUID;

public interface ICreateShopService {

  void process(CreateShopRequest request, UUID currentUserId, String currentRoleName);
}
