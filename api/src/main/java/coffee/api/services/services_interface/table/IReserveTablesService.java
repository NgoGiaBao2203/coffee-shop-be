package coffee.api.services.services_interface.table;

import coffee.api.dto.request.table.ReserveTablesRequest;
import java.util.UUID;

public interface IReserveTablesService {

  void process(
      ReserveTablesRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
