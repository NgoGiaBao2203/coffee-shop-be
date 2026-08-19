package coffee.api.services.services_interface.shop_branch;

import coffee.api.dto.request.shop_branch.CreateShopBranchRequest;
import java.util.UUID;

public interface ICreateShopBranchService {
  void process(CreateShopBranchRequest request, String currentUserRoleName, UUID currentUserId);
}
