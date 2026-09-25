package coffee.api.services.services_interface.shop;

import coffee.api.dto.request.shop.DeleteShopRequest;
import java.util.UUID;

public interface IDeleteShopService {

  void process(DeleteShopRequest request, UUID currentUserId);
}
