package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.EditTablesRequest;
import coffee.api.dto.response.base_response.ErrorDetail;
import coffee.api.enums.ResponseCode;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.EditTablesMapper;
import coffee.api.services.services_implement.table.common.TableValidator;
import coffee.api.services.services_interface.table.IEditTablesService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditTablesServiceImpl implements IEditTablesService {

  private final CommonMapper commonMapper;
  private final EditTablesMapper editTablesMapper;
  private final TableValidator tableValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {

    // 1 & 2. Verify request, IDs and table number validity
    validateRequest(request);

    // 3. Verify shop exists
    tableValidator.validateShopExists(request.getShopId());

    // 4. Verify authorization and active assignment for MANAGER
    tableValidator.verifyManagerAuthorization(
        currentUserId, currentUserRoleName, currentUserShopId, request.getShopId());

    // 5. Verify table exists and role/shop access
    tableValidator.validateTableExists(
        request.getTableId(), currentUserRoleName, currentUserShopId);

    // 6. Verify table belongs to the specified shop branch (prevent cross-branch transfer)
    validateTableBelongsToShop(request.getTableId(), request.getShopId());

    // 7. Trim description
    request.setDescription(tableValidator.sanitizeDescription(request.getDescription()));

    // 8. Verify duplicate table number in shop
    verifyTableNumberUniqueness(
        request.getTableId(),
        request.getTableNumber(),
        request.getShopId(),
        currentUserRoleName,
        currentUserShopId);

    // 9. Update table record and verify affected rows
    updateTableRecord(request, currentUserRoleName, currentUserShopId);
  }

  private void validateRequest(EditTablesRequest request) {
    if (request == null || request.getShopId() == null) {
      throw new InvalidRequestException("Shop ID is required");
    }
    if (request.getTableId() == null) {
      throw new InvalidRequestException("Table ID is required");
    }
    if (request.getTableNumber() == null || request.getTableNumber() <= 0) {
      throw new InvalidRequestException("Table number must be greater than 0");
    }
  }

  private void validateTableBelongsToShop(UUID tableId, UUID shopId) {
    boolean isTableInShop = commonMapper.checkTableExistsInShop(tableId, shopId);
    if (!isTableInShop) {
      throw new InvalidRequestException("Table does not belong to this shop branch");
    }
  }

  private void verifyTableNumberUniqueness(
      UUID tableId,
      Integer tableNumber,
      UUID shopId,
      String currentUserRoleName,
      UUID currentUserShopId) {

    boolean isTableNumberExisted =
        commonMapper.checkTableNumberExisted(
            tableId, tableNumber, shopId, currentUserRoleName, currentUserShopId);

    List<ErrorDetail> errors = new ArrayList<>();
    if (isTableNumberExisted) {
      ErrorDetail error = new ErrorDetail();
      error.setErrorCode(ResponseCode.CONFLICT.getCode());
      error.setMessage("Table number already exists in this shop");
      errors.add(error);
    }

    if (!errors.isEmpty()) {
      throw new InvalidRequestWithErrorDetailsException("Invalid request", errors);
    }
  }

  private void updateTableRecord(
      EditTablesRequest request, String currentUserRoleName, UUID currentUserShopId) {

    int affectedRows =
        editTablesMapper.updateTable(request, currentUserRoleName, currentUserShopId);
    if (affectedRows == 0) {
      throw new DataNotFoundException(
          "Table has already been deleted or modified by another request", request.getTableId());
    }
  }
}
