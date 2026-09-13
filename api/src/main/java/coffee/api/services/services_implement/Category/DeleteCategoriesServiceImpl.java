package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.DeleteCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteCategoryMapper;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.category.IDeleteCategoriesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
      DeleteCategoriesRequest request, String currentUserRoleName, UUID currentUserShopId) {

    UUID currentProfileId = getCurrentProfileId();

    // 1. Verify manager role belongs to active shop branch
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop branch");
      }
      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentProfileId, currentUserShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop branch");
      }
    }

    // 2. Verify category existence and role/shop access
    boolean isCategoryExisted =
        commonMapper.checkCategoryExisted(
            request.getCategoryId(), currentUserRoleName, currentUserShopId);
    if (!isCategoryExisted) {
      throw new DataNotFoundException("Data not found", request.getCategoryId());
    }

    // 3. Cascade soft-delete drink variants and prices first to prevent orphan records
    deleteCategoryMapper.softDeleteDrinkDetailsByCategory(
        request.getCategoryId(), currentUserRoleName, currentUserShopId, currentProfileId);

    // 4. Cascade soft-delete drinks under the category
    deleteCategoryMapper.softDeleteDrinksByCategory(
        request.getCategoryId(), currentUserRoleName, currentUserShopId, currentProfileId);

    // 5. Soft-delete the category record
    deleteCategoryMapper.softDeleteCategory(
        request.getCategoryId(), currentUserRoleName, currentUserShopId, currentProfileId);
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
