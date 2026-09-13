package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateCategoriesMapper;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.category.ICreateCategoriesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCategoriesServiceImpl implements ICreateCategoriesService {

  private final CommonMapper commonMapper;
  private final CreateCategoriesMapper createCategoriesMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      CreateCategoriesRequest request, String currentUserRoleName, UUID currentUserShopId) {

    UUID currentUserId = getCurrentProfileId();

    // 1. Verify target shop input
    if (request == null || request.getShopId() == null) {
      throw new InvalidRequestException("Shop ID is required");
    }

    // 2. Validate target shop exists and is active (both OWNER and MANAGER)
    boolean isShopExisted = commonMapper.checkShopExisted(request.getShopId());
    if (!isShopExisted) {
      throw new DataNotFoundException("Data not found", request.getShopId());
    }

    // 3. Verify authorization and active assignment for MANAGER
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop branch");
      }

      if (!currentUserShopId.equals(request.getShopId())) {
        throw new InvalidRequestException("You do not have permission to access this shop branch");
      }

      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop branch");
      }
    }

    // 4. Trim category name and verify uniqueness within the target shop
    String trimmedCategoryName =
        request.getCategoryName() != null ? request.getCategoryName().trim() : "";
    request.setCategoryName(trimmedCategoryName);

    boolean isCategoryExisted =
        commonMapper.checkCategoryNameExisted(null, trimmedCategoryName, request.getShopId());
    if (isCategoryExisted) {
      throw new UserExistException("Category name is existed");
    }

    // 5. Insert new category
    createCategoriesMapper.createCategories(request, currentUserRoleName, currentUserShopId);
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
