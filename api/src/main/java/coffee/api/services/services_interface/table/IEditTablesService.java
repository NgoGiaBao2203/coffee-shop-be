package coffee.api.services.services_interface.table;

import coffee.api.dto.request.table.EditTablesRequest;
import java.util.UUID;

public interface IEditTablesService {
  void process(
      EditTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId);
}
