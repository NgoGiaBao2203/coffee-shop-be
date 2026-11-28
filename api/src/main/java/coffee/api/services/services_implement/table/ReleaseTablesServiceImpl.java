package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.ReleaseTablesRequest;
import coffee.api.services.services_implement.table.common.TableStatusChanger;
import coffee.api.services.services_interface.table.IReleaseTablesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReleaseTablesServiceImpl implements IReleaseTablesService {
  private final TableStatusChanger tableStatusChanger;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      ReleaseTablesRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    tableStatusChanger.change(
        request.getTableId(),
        TableStatusChanger.STATUS_AVAILABLE,
        currentUserRoleName,
        currentUserShopId,
        currentUserId);
  }
}
