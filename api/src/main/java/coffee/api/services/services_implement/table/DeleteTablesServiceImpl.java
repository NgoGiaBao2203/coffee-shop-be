package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.DeleteTablesRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteTablesMapper;
import coffee.api.services.services_implement.table.common.TableValidator;
import coffee.api.services.services_interface.table.IDeleteTablesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteTablesServiceImpl implements IDeleteTablesService {

  private final DeleteTablesMapper deleteTablesMapper;
  private final TableValidator tableValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      DeleteTablesRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {

    // 1. Verify table ID input
    validateRequest(request);

    // 2. Verify manager role belongs to active shop branch
    tableValidator.verifyManagerAssignment(currentUserId, currentUserRoleName, currentUserShopId);

    // 3. Verify table existence and role/shop access
    tableValidator.validateTableExists(
        request.getTableId(), currentUserRoleName, currentUserShopId);

    // 4. Preserve open invoice data before deleting table
    verifyNoPendingInvoices(request.getTableId());

    // 5. Soft-delete the table record and verify affected rows
    softDeleteTableRecord(
        request.getTableId(), currentUserRoleName, currentUserShopId, currentUserId);
  }

  private void validateRequest(DeleteTablesRequest request) {
    if (request == null || request.getTableId() == null) {
      throw new InvalidRequestException("Table ID is required");
    }
  }

  private void verifyNoPendingInvoices(UUID tableId) {
    int pendingInvoices = deleteTablesMapper.countPendingInvoicesByTable(tableId);
    if (pendingInvoices > 0) {
      throw new InvalidRequestException("Can not delete table with pending invoices");
    }
  }

  private void softDeleteTableRecord(
      UUID tableId, String currentUserRoleName, UUID currentUserShopId, UUID currentUserId) {

    int affectedRows =
        deleteTablesMapper.softDeleteTable(
            tableId, currentUserRoleName, currentUserShopId, currentUserId);
    if (affectedRows == 0) {
      throw new DataNotFoundException(
          "Table has already been deleted or modified by another request", tableId);
    }
  }
}
