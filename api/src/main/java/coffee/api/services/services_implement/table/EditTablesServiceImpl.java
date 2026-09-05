package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.EditTablesRequest;
import coffee.api.dto.response.base_response.ErrorDetail;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.EditTablesMapper;
import coffee.api.services.services_interface.table.IEditTablesService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditTablesServiceImpl implements IEditTablesService {
  private final CommonMapper commonMapper;
  private final EditTablesMapper editTablesMapper;

  @Override
  public void process(
      EditTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {
    validateData(request, currentUserRoleName, currentUserId, currentUserShopId);
    editTablesMapper.updateTable(request, currentUserRoleName, currentUserShopId);
  }

  private void validateData(
      EditTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {
    List<ErrorDetail> errors = new ArrayList<>();

    // Validate table exists and user has access
    Boolean isTableExisted =
        commonMapper.checkTableExisted(
            request.getTableId(), currentUserRoleName, currentUserShopId);

    if (!isTableExisted) {
      throw new DataNotFoundException("Table not found", request.getTableId());
    }

    // Validate shop exists
    Boolean isShopExisted = commonMapper.checkShopExisted(request.getShopId());
    if (!isShopExisted) {
      throw new DataNotFoundException("Shop not found", request.getShopId());
    }

    // For MANAGER, validate shop belongs to them
    if (currentUserRoleName.equals(Roles.MANAGER.getValue())) {
      Boolean isShopIdIsExist = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);

      if (!isShopIdIsExist) {
        throw new DataNotFoundException("Shop Id not found", currentUserShopId);
      }

      // MANAGER can only edit tables in their own shop
      if (!request.getShopId().equals(currentUserShopId)) {
        throw new DataNotFoundException("Access denied for this shop", request.getShopId());
      }
    }

    // Validate table number uniqueness in the shop (excluding current table)
    Boolean isTableNumberExisted =
        commonMapper.checkTableNumberExisted(
            request.getTableId(),
            request.getTableNumber(),
            request.getShopId(),
            currentUserRoleName,
            currentUserShopId);

    if (isTableNumberExisted) {
      ErrorDetail error = new ErrorDetail();
      error.setErrorCode(ResponseCode.CONFLICT.getCode());
      error.setMessage("Table number already exists in this shop");
      errors.add(error);
    }

    // Validate status if provided
    if (request.getStatus() != null && (request.getStatus() < 1 || request.getStatus() > 3)) {
      ErrorDetail error = new ErrorDetail();
      error.setErrorCode(ResponseCode.BAD_REQUEST.getCode());
      error.setMessage("Status must be 1 (available), 2 (occupied), or 3 (reserved)");
      errors.add(error);
    }

    if (!errors.isEmpty()) {
      throw new InvalidRequestWithErrorDetailsException("Invalid request", errors);
    }
  }
}
