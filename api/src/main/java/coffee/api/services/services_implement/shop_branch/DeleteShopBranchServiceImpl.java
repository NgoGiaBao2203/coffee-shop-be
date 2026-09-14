package coffee.api.services.services_implement.shop_branch;

import coffee.api.dto.request.shop_branch.DeleteShopBranchRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteShopBranchMapper;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.shop_branch.IDeleteShopBranchService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    UUID currentProfileId = getCurrentProfileId();

    // 1. Verify existence of the target shop branch
    Boolean isShopExisted = deleteShopBranchMapper.checkShopExistedById(shopId);
    if (!Boolean.TRUE.equals(isShopExisted)) {
      throw new DataNotFoundException("Data not found", shopId);
    }

    // 2. Prevent branch deletion if pending or unpaid invoices exist
    int pendingInvoices = deleteShopBranchMapper.countActiveInvoicesByShopId(shopId);
    if (pendingInvoices > 0) {
      throw new InvalidRequestException("Can not delete branch with pending invoices");
    }

    // 3. Cascade soft-delete drink variants and prices first to prevent orphan records
    deleteShopBranchMapper.softDeleteDrinkDetailsByShopId(shopId, currentProfileId);

    // 4. Cascade soft-delete drinks and their parent categories
    deleteShopBranchMapper.softDeleteDrinksByShopId(shopId, currentProfileId);
    deleteShopBranchMapper.softDeleteCategoriesByShopId(shopId, currentProfileId);

    // 5. Cascade soft-delete dining tables
    deleteShopBranchMapper.softDeleteTablesByShopId(shopId, currentProfileId);

    // 6. Disassociate staff permissions from this shop (do not delete staff profiles)
    deleteShopBranchMapper.softDeleteProfileShopsByShopId(shopId, currentProfileId);

    // 7. Soft-delete the target shop branch record
    deleteShopBranchMapper.softDeleteShopBranch(shopId, currentProfileId);
  }

  private UUID getCurrentProfileId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof CustomUserDetail userDetail) {
      return userDetail.getUserId();
    }
    return null;
  }
}
