package coffee.api.services.services_interface.table;

import coffee.api.dto.request.table.SearchTablesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.TableResult;
import java.util.UUID;

public interface IGetTablesService {
  PageResponse<TableResult> process(
      SearchTablesRequest request, String currentUserRoleName, UUID currentUserId);
}
