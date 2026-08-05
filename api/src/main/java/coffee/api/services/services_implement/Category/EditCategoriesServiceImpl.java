package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.EditCategoriesRequest;
import coffee.api.dto.response.base_response.ErrorDetail;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateCategoryMapper;
import coffee.api.services.services_interface.category.IEditCategoriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EditCategoriesServiceImpl implements IEditCategoriesService {
  private final CommonMapper commonMapper;
  private final UpdateCategoryMapper updateCategoryMapper;

  @Override
  public void process(
    EditCategoriesRequest request,
    String currentUserRoleName,
    UUID currentUserId,
    UUID currentUserShopId
  ) {
    validateData(
      request,
      currentUserRoleName,
      currentUserId,
      currentUserShopId
    );
    updateCategoryMapper.updateCategory(
      request,
      currentUserRoleName,
      currentUserShopId
    );
  }

  private void validateData(
    EditCategoriesRequest request,
    String currentUserRoleName,
    UUID currentUserId,
    UUID currentUserShopId
  ) {
    List<ErrorDetail> errors = new ArrayList<>();
    Boolean isCategoryExisted = commonMapper.checkCategoryExisted(
      request.getCategoryId(),
      currentUserRoleName,
      currentUserShopId
    );

    if (!isCategoryExisted) {
      throw new DataNotFoundException(
        "Data not found",
        request.getCategoryId()
      );
    }

    if (currentUserRoleName.equals(Roles.MANAGER.getValue())) {
      Boolean isShopIdIsExist = commonMapper.checkShopIdIsExisted(
        currentUserId,
        currentUserShopId
      );

      if (!isShopIdIsExist) {
        throw new DataNotFoundException("Shop Id not found", currentUserShopId);
      }
    }
    Boolean isCategoryNameExisted = commonMapper.checkCategoryNameExisted(
      request.getCategoryId(),
      request.getCategoryName(),
      currentUserRoleName,
      currentUserShopId
    );

    if (isCategoryNameExisted) {
      ErrorDetail error = new ErrorDetail();
      error.setErrorCode(ResponseCode.CONFLICT.getCode());
      error.setMessage("Category name already exists");
      errors.add(error);
    }

    if(!errors.isEmpty()) {
      throw new InvalidRequestWithErrorDetailsException("Invalid request", errors);
    }
  }
}
