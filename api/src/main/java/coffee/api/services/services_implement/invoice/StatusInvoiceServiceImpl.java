package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.StatusInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdateInvoiceStatusMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import coffee.api.services.services_interface.invoice.IStatusInvoiceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatusInvoiceServiceImpl implements IStatusInvoiceService {
  private final UpdateInvoiceStatusMapper updateInvoiceStatusMapper;
  private final InvoiceValidator invoiceValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      StatusInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {
    // 1. Verify target status is paid or cancelled
    validateTargetStatus(request.getStatus());

    // 2. Lock invoice and verify shop permission, ownership and unpaid status
    InvoiceResult invoice =
        invoiceValidator.validateInvoiceForStatusChange(
            request.getInvoiceId(), currentUserRoleName, currentUserShopId, currentUserId);

    // 3. Update only an unpaid invoice in the validated shop
    if (updateInvoiceStatusMapper.updateInvoiceStatus(
            request.getInvoiceId(), invoice.getShopId(), request.getStatus())
        != 1) {
      throw new InvalidRequestException(
          "Invoice status cannot be updated. Please reload and try again");
    }

    // 4. Release the table only after all invoices on it are paid or cancelled
    invoiceValidator.releaseInvoiceTable(invoice.getTableId(), invoice.getShopId());
  }

  /** Only allow payment or cancellation as a target status. */
  private void validateTargetStatus(Integer status) {
    if (status == null || (status != 1 && status != 2)) {
      throw new InvalidRequestException("Invoice status must be 1 (paid) or 2 (cancelled)");
    }
  }
}
