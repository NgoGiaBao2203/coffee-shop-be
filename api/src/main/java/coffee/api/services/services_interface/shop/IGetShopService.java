package coffee.api.services.services_interface.shop;

import coffee.api.dto.request.shop.SearchShopRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.ShopResult;
import java.util.UUID;

public interface IGetShopService {
  PageResponse<ShopResult> process(
      SearchShopRequest request, String currentUserRoleName, UUID currentUserId);
}
