package coffee.api.services.services_interface.shop;

import coffee.api.dto.request.shop.EditShopRequest;
import java.util.UUID;

public interface IEditShopService {
  void process(UUID shopId, EditShopRequest request);
}
