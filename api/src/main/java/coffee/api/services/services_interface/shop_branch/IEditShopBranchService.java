package coffee.api.services.services_interface.shop_branch;

import coffee.api.dto.request.shop_branch.EditShopBranchRequest;
import java.util.UUID;

public interface IEditShopBranchService {
  void process(EditShopBranchRequest request, String currentUserRoleName, UUID currentUserId);
}
