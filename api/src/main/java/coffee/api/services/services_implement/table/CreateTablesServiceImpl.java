package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.CreateTablesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateTablesMapper;
import coffee.api.services.services_interface.table.ICreateTablesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateTablesServiceImpl implements ICreateTablesService {
  private final CommonMapper commonMapper;
  private final CreateTablesMapper createTablesMapper;

  @Override
  public void process(
      CreateTablesRequest request, String currentUserRoleName, UUID currentUserShopId) {
    // Validate role - only OWNER and MANAGER can create tables
    if (!Roles.OWNER.getValue().equals(currentUserRoleName)
        && !Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      throw new InvalidRequestException("Only OWNER and MANAGER can create tables");
    }

    // Validate shop ID for MANAGER
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (!currentUserShopId.equals(request.getShopId())) {
        throw new InvalidRequestException("Shop Ids are not match profile");
      }
    }

    // Check if shop exists
    Boolean isShopExisted = commonMapper.checkShopExisted(request.getShopId());
    if (!isShopExisted) {
      throw new DataNotFoundException("Data not found", request.getShopId());
    }

    // Check if table number already exists in the shop
    Boolean isTableNumberExisted =
        createTablesMapper.checkTableNumberExisted(request.getShopId(), request.getTableNumber());
    if (isTableNumberExisted) {
      throw new InvalidRequestException("Table number already exists in this shop");
    }

    // Validate status
    if (request.getStatus() == null || request.getStatus() < 1 || request.getStatus() > 3) {
      throw new InvalidRequestException(
          "Status must be 1 (available), 2 (occupied), or 3 (reserved)");
    }

    createTablesMapper.createTable(
        request.getTableNumber(),
        request.getDescription(),
        request.getStatus(),
        request.getShopId());
  }
}
