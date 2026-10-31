package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.CreateTablesRequest;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateTablesMapper;
import coffee.api.services.services_implement.table.common.TableValidator;
import coffee.api.services.services_interface.table.ICreateTablesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateTablesServiceImpl implements ICreateTablesService {

  private final CommonMapper commonMapper;
  private final CreateTablesMapper createTablesMapper;
  private final TableValidator tableValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      CreateTablesRequest request,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {

    // 1 & 2. Verify request and table number validity
    validateRequest(request);

    // 3. Verify shop exists
    tableValidator.validateShopExists(request.getShopId());

    // 4. Verify authorization and active assignment for MANAGER
    tableValidator.verifyManagerAuthorization(
        currentUserId, currentUserRoleName, currentUserShopId, request.getShopId());

    // 5. Trim description and sanitize input
    request.setDescription(tableValidator.sanitizeDescription(request.getDescription()));

    // 6. Verify table number uniqueness in shop
    verifyTableNumberUniqueness(
        request.getTableNumber(), request.getShopId(), currentUserRoleName, currentUserShopId);

    // 7. Insert new table record
    createTablesMapper.createTable(request, currentUserRoleName, currentUserShopId);
  }

  private void validateRequest(CreateTablesRequest request) {
    if (request == null || request.getShopId() == null) {
      throw new InvalidRequestException("Shop ID is required");
    }
    if (request.getTableNumber() == null || request.getTableNumber() <= 0) {
      throw new InvalidRequestException("Table number must be greater than 0");
    }
  }

  private void verifyTableNumberUniqueness(
      Integer tableNumber, UUID shopId, String currentUserRoleName, UUID currentUserShopId) {

    boolean isTableNumberExisted =
        commonMapper.checkTableNumberExisted(
            null, tableNumber, shopId, currentUserRoleName, currentUserShopId);
    if (isTableNumberExisted) {
      throw new InvalidRequestException("Table number already exists in this shop");
    }
  }
}
