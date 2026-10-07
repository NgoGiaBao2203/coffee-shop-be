package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.EditCategoriesRequest;
import coffee.api.dto.response.base_response.ErrorDetail;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateCategoryMapper;
import coffee.api.services.services_interface.category.IEditCategoriesService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditCategoriesServiceImpl implements IEditCategoriesService {

  private final CommonMapper commonMapper;
  private final UpdateCategoryMapper updateCategoryMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditCategoriesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {

    validateData(request, currentUserRoleName, currentUserId, currentUserShopId);
    updateCategoryMapper.updateCategory(request, currentUserRoleName, currentUserShopId);
  }

  private void validateData(
      EditCategoriesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {

    // 1. Verify request, shop ID and category ID
    if (request == null || request.getShopId() == null) {
      throw new InvalidRequestException("Shop ID is required");
    }
    if (request.getCategoryId() == null) {
      throw new InvalidRequestException("Category ID is required");
    }

    // 2. Verify shop exists
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

    // 4. Verify category exists and role/shop access
    boolean isCategoryExisted =
        commonMapper.checkCategoryExisted(
            request.getCategoryId(), currentUserRoleName, currentUserShopId);
    if (!isCategoryExisted) {
      throw new DataNotFoundException("Data not found", request.getCategoryId());
    }

    // 5. OWNER can access any shop, but the category must actually belong to the shop it
    if (Roles.OWNER.getValue().equals(currentUserRoleName)) {
      boolean categoryBelongsToRequestedShop =
          commonMapper.checkCategoryExistsInShop(request.getCategoryId(), request.getShopId());
      if (!categoryBelongsToRequestedShop) {
        throw new DataNotFoundException("Data not found", request.getCategoryId());
      }
    }

    // 6. Trim category name and verify empty
    String trimmedCategoryName =
        request.getCategoryName() != null ? request.getCategoryName().trim() : "";
    if (trimmedCategoryName.isEmpty()) {
      throw new InvalidRequestException("Category name is required");
    }
    request.setCategoryName(trimmedCategoryName);

    // 7. Verify duplicate category name in shop
    boolean isCategoryNameExisted =
        commonMapper.checkCategoryNameExisted(
            request.getCategoryId(), trimmedCategoryName, request.getShopId());

    List<ErrorDetail> errors = new ArrayList<>();
    if (isCategoryNameExisted) {
      ErrorDetail error = new ErrorDetail();
      error.setErrorCode(ResponseCode.CONFLICT.getCode());
      error.setMessage("Category name already exists");
      errors.add(error);
    }

    if (!errors.isEmpty()) {
      throw new InvalidRequestWithErrorDetailsException("Invalid request", errors);
    }
  }
}
