package coffee.api.services.services_interface.shop_branch;

import coffee.api.dto.request.shop_branch.EditShopBranchRequest;

public interface IEditShopBranchService {
  void process(EditShopBranchRequest request);
}
