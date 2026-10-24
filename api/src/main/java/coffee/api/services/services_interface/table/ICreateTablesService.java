package coffee.api.services.services_interface.table;

import coffee.api.dto.request.table.CreateTablesRequest;
import java.util.UUID;

public interface ICreateTablesService {
  void process(CreateTablesRequest request, String currentUserRoleName, UUID currentUserShopId);
}
