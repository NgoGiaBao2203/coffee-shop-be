package coffee.api.services.services_interface.table;

import coffee.api.dto.request.table.DeleteTablesRequest;
import java.util.UUID;

public interface IDeleteTablesService {
  void process(
      DeleteTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId);
}
