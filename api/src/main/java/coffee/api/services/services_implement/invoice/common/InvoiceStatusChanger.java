package coffee.api.services.services_implement.invoice.common;

import coffee.api.dto.result.InvoiceResult;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdateInvoiceStatusMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Moves an unpaid invoice to a final status chosen by the server (paid or cancelled). */
@Component
@RequiredArgsConstructor
public class InvoiceStatusChanger {
  public static final int STATUS_PAID = 1;
  public static final int STATUS_CANCELLED = 2;

  private final UpdateInvoiceStatusMapper updateInvoiceStatusMapper;
  private final InvoiceValidator invoiceValidator;

  public void change(
      UUID invoiceId,
      int targetStatus,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Lock invoice and verify shop permission, ownership and unpaid status
    InvoiceResult invoice =
        invoiceValidator.validateInvoiceForStatusChange(
            invoiceId, currentUserRoleName, currentUserShopId, currentUserId);

    // 2. Update only an unpaid invoice in the validated shop
    if (updateInvoiceStatusMapper.updateInvoiceStatus(invoiceId, invoice.getShopId(), targetStatus)
        != 1) {
      throw new InvalidRequestException(
          "Invoice status cannot be updated. Please reload and try again");
    }

    // 3. Release the table only after all invoices on it are paid or cancelled
    invoiceValidator.releaseInvoiceTable(invoice.getTableId(), invoice.getShopId());
  }
}
