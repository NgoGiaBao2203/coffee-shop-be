package coffee.api.services.services_implement.table.common;

import coffee.api.constants.ErrorMessage;
import coffee.api.dto.result.TableResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateTableStatusMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Moves a table to a status chosen by the server (available, occupied or reserved). */
@Component
@RequiredArgsConstructor
public class TableStatusChanger {
  public static final int STATUS_AVAILABLE = 1;
  public static final int STATUS_OCCUPIED = 2;
  public static final int STATUS_RESERVED = 3;

  private final UpdateTableStatusMapper updateTableStatusMapper;
  private final CommonMapper commonMapper;

  public void change(
      UUID tableId,
      int targetStatus,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. MANAGER and STAFF must be assigned to a shop branch
    verifyShopAssignment(currentUserRoleName, currentUserShopId);

    // 2. Lock the table inside the caller's scope (not found when outside it)
    TableResult table = lockTable(tableId, currentUserRoleName, currentUserShopId);

    // 3. MANAGER and STAFF must still be an active member of the table's shop
    verifyShopMembership(table.getShopId(), currentUserRoleName, currentUserId);

    // 4. Reject a change to the status the table already has
    if (table.getStatus() != null && table.getStatus() == targetStatus) {
      throw new InvalidRequestException("Table is already in this status");
    }

    // 5. Keep the table occupied while unpaid invoices still use it
    if (targetStatus != STATUS_OCCUPIED && commonMapper.hasUnpaidTableInvoices(tableId)) {
      throw new InvalidRequestException(ErrorMessage.TABLE_HAS_PENDING_INVOICES);
    }

    // 6. Update the status, guarded again in SQL against concurrent changes
    if (updateTableStatusMapper.updateTableStatus(tableId, table.getShopId(), targetStatus) != 1) {
      throw new InvalidRequestException(
          "Table status cannot be updated. Please reload and try again");
    }
  }

  private void verifyShopAssignment(String currentUserRoleName, UUID currentUserShopId) {
    if (!Roles.OWNER.getValue().equals(currentUserRoleName) && currentUserShopId == null) {
      throw new InvalidRequestException(ErrorMessage.USER_NOT_ASSIGNED_TO_SHOP);
    }
  }

  private TableResult lockTable(UUID tableId, String currentUserRoleName, UUID currentUserShopId) {
    TableResult table =
        updateTableStatusMapper.lockTable(tableId, currentUserRoleName, currentUserShopId);
    if (table == null) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, tableId);
    }
    return table;
  }

  private void verifyShopMembership(UUID shopId, String currentUserRoleName, UUID currentUserId) {
    if (Roles.OWNER.getValue().equals(currentUserRoleName)) {
      return;
    }
    if (!Boolean.TRUE.equals(commonMapper.checkShopIdIsExisted(currentUserId, shopId))) {
      throw new InvalidRequestException(ErrorMessage.SHOP_ACCESS_DENIED);
    }
  }
}
