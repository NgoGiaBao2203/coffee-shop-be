package coffee.api.services.services_interface.shop_branch;

import coffee.api.dto.request.shop_branch.DeleteShopBranchRequest;

public interface IDeleteShopBranchService {

  void process(DeleteShopBranchRequest request);
}
