package coffee.api.services.services_interface.shop_branch;

import coffee.api.dto.request.shop_branch.SearchShopBranchRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.ShopBranchResult;
import java.util.UUID;

public interface IGetShopBranchService {
  PageResponse<ShopBranchResult> process(
      SearchShopBranchRequest request, String currentUserRoleName, UUID currentUserId);
}
