package coffee.api.services.services_implement.shop;

import coffee.api.dto.request.shop.DeleteShopRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteShopMapper;
import coffee.api.services.services_interface.shop.IDeleteShopService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteShopServiceImpl implements IDeleteShopService {

  private final DeleteShopMapper deleteShopMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(DeleteShopRequest request, UUID currentUserId) {

    UUID shopId = request.getShopId();

    // 1. Verify existence of the target shop
    Boolean isShopExisted = deleteShopMapper.checkShopExistedById(shopId);
    if (!Boolean.TRUE.equals(isShopExisted)) {
      throw new DataNotFoundException("Data not found", shopId);
    }

    // 2. Validate blocking business constraints
    List<String> blockingReasons = new ArrayList<>();

    int activeStaffCount = deleteShopMapper.countActiveStaffByShopId(shopId);
    if (activeStaffCount > 0) {
      blockingReasons.add("active staff");
    }

    int pendingInvoices = deleteShopMapper.countActiveInvoicesByShopId(shopId);
    if (pendingInvoices > 0) {
      blockingReasons.add("pending invoices");
    }

    if (!blockingReasons.isEmpty()) {
      throw new InvalidRequestException(
          "Cannot delete shop with " + String.join(" and ", blockingReasons));
    }

    // 3. Cascade soft-delete drink variants and prices first to prevent orphan records
    deleteShopMapper.softDeleteDrinkDetailsByShopId(shopId, currentUserId);

    // 4. Cascade soft-delete drinks and their parent categories
    deleteShopMapper.softDeleteDrinksByShopId(shopId, currentUserId);
    deleteShopMapper.softDeleteCategoriesByShopId(shopId, currentUserId);

    // 5. Cascade soft-delete dining tables
    deleteShopMapper.softDeleteTablesByShopId(shopId, currentUserId);

    // 6. Soft-delete the target shop record
    deleteShopMapper.softDeleteShop(shopId, currentUserId);
  }
}
