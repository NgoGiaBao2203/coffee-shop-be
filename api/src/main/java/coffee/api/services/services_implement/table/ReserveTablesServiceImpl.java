package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.ReserveTablesRequest;
import coffee.api.services.services_implement.table.common.TableStatusChanger;
import coffee.api.services.services_interface.table.IReserveTablesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReserveTablesServiceImpl implements IReserveTablesService {
  private final TableStatusChanger tableStatusChanger;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      ReserveTablesRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    tableStatusChanger.change(
        request.getTableId(),
        TableStatusChanger.STATUS_RESERVED,
        currentUserRoleName,
        currentUserShopId,
        currentUserId);
  }
}
