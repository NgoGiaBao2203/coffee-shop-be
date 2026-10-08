package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.DeleteCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteCategoryMapper;
import coffee.api.services.services_interface.category.IDeleteCategoriesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCategoriesServiceImpl implements IDeleteCategoriesService {

  private final CommonMapper commonMapper;
  private final DeleteCategoryMapper deleteCategoryMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      DeleteCategoriesRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Verify category ID input
    if (request == null || request.getCategoryId() == null) {
      throw new InvalidRequestException("Category ID is required");
    }

    // 2. Verify manager role belongs to active shop branch
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop branch");
      }
      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop branch");
      }
    }

    // 3. Verify category existence and role/shop access
    boolean isCategoryExisted =
        commonMapper.checkCategoryExisted(
            request.getCategoryId(), currentUserRoleName, currentUserShopId);
    if (!isCategoryExisted) {
      throw new DataNotFoundException("Data not found", request.getCategoryId());
    }

    // 4. Preserve open invoice data before deleting drinks or their details
    int pendingInvoices =
        deleteCategoryMapper.countPendingInvoicesByCategory(request.getCategoryId());
    if (pendingInvoices > 0) {
      throw new InvalidRequestException("Can not delete category with pending invoices");
    }

    // 5. Cascade soft-delete drink variants and prices first to prevent orphan records
    deleteCategoryMapper.softDeleteDrinkDetailsByCategory(
        request.getCategoryId(), currentUserRoleName, currentUserShopId, currentUserId);

    // 6. Cascade soft-delete drinks under the category
    deleteCategoryMapper.softDeleteDrinksByCategory(
        request.getCategoryId(), currentUserRoleName, currentUserShopId, currentUserId);

    // 7. Soft-delete the category record and verify affected rows
    int affectedRows =
        deleteCategoryMapper.softDeleteCategory(
            request.getCategoryId(), currentUserRoleName, currentUserShopId, currentUserId);
    if (affectedRows == 0) {
      throw new DataNotFoundException(
          "Category has already been deleted or modified by another request",
          request.getCategoryId());
    }
  }
}
