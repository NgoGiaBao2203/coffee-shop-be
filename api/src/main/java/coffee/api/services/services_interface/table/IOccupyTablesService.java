package coffee.api.services.services_interface.table;

import coffee.api.dto.request.table.OccupyTablesRequest;
import java.util.UUID;

public interface IOccupyTablesService {

  void process(
      OccupyTablesRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
