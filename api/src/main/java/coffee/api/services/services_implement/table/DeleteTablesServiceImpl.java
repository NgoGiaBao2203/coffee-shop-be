package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.DeleteTablesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteTablesMapper;
import coffee.api.services.services_interface.table.IDeleteTablesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteTablesServiceImpl implements IDeleteTablesService {
  private final CommonMapper commonMapper;
  private final DeleteTablesMapper deleteTablesMapper;

  @Override
  public void process(
      DeleteTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {
    validateData(request, currentUserRoleName, currentUserId, currentUserShopId);
    deleteTablesMapper.deleteTable(request.getTableId(), currentUserRoleName, currentUserShopId);
  }

  private void validateData(
      DeleteTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {
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

      // MANAGER can only delete tables in their own shop
      if (!request.getShopId().equals(currentUserShopId)) {
        throw new DataNotFoundException("Access denied for this shop", request.getShopId());
      }
    }
  }
}
