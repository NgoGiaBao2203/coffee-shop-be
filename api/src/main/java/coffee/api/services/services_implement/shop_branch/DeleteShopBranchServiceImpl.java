package coffee.api.services.services_implement.shop_branch;

import coffee.api.dto.request.shop_branch.DeleteShopBranchRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteShopBranchMapper;
import coffee.api.services.services_interface.shop_branch.IDeleteShopBranchService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteShopBranchServiceImpl implements IDeleteShopBranchService {

  private final DeleteShopBranchMapper deleteShopBranchMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(DeleteShopBranchRequest request) {
    UUID shopId = request.getShopId();

    // 1. Verify if the target shop branch exists
    Boolean isShopExisted = deleteShopBranchMapper.checkShopExistedById(shopId);
    if (!Boolean.TRUE.equals(isShopExisted)) {
      throw new DataNotFoundException("Data not found", shopId);
    }

    // 2. Prevent deletion if there are active / unpaid invoices
    int pendingInvoices = deleteShopBranchMapper.countActiveInvoicesByShopId(shopId);
    if (pendingInvoices > 0) {
      throw new InvalidRequestException("Can not delete branch pending invoices");
    }

    // 3. Cascade soft-delete related drink categories
    deleteShopBranchMapper.softDeleteCategoriesByShopId(shopId);

    // 4. Cascade soft-delete related drinks
    deleteShopBranchMapper.softDeleteDrinksByShopId(shopId);

    // 5. Soft-delete the shop branch itself
    deleteShopBranchMapper.softDeleteShopBranch(shopId);
  }
}
