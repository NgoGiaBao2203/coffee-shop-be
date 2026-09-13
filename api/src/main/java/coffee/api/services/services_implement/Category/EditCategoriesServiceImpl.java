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

    // 1. Verify target shop exists and is active (both OWNER & MANAGER)
    boolean isShopExisted = commonMapper.checkShopExisted(request.getShopId());
    if (!isShopExisted) {
      throw new DataNotFoundException("Data not found", request.getShopId());
    }

    // 2. Verify manager has permission to access the target shop
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (request.getShopId() == null) {
        throw new InvalidRequestException("Shop branch is required");
      }
      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, request.getShopId());
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

    // 4. Verify duplicate category name in the target shop
    String trimmedCategoryName = request.getCategoryName().trim();
    request.setCategoryName(trimmedCategoryName);

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
