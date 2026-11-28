package coffee.api.services.services_interface.table;

import coffee.api.dto.request.table.ReleaseTablesRequest;
import java.util.UUID;

public interface IReleaseTablesService {

  void process(
      ReleaseTablesRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
