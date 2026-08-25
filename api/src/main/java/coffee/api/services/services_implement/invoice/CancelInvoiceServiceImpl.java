package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.CancelInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CancelInvoiceMapper;
import coffee.api.services.services_interface.invoice.ICancelInvoiceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelInvoiceServiceImpl implements ICancelInvoiceService {

  private final CancelInvoiceMapper cancelInvoiceMapper;

  @Override
  @Transactional
  public void process(
      CancelInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Check invoice exists
    InvoiceResult existingInvoice = cancelInvoiceMapper.findInvoiceById(request.getInvoiceId());
    if (existingInvoice == null) {
      throw new DataNotFoundException("Invoice not found", request.getInvoiceId());
    }

    // 2. Only OWNER, MANAGER, STAFF can cancel
    if (!Roles.OWNER.name().equalsIgnoreCase(currentUserRoleName)
        && !Roles.MANAGER.name().equalsIgnoreCase(currentUserRoleName)
        && !Roles.STAFF.name().equalsIgnoreCase(currentUserRoleName)) {
      throw new InvalidRequestException("You do not have permission to cancel this invoice");
    }

    // 3. Non-owner can only cancel invoices from their own shop
    if (!Roles.OWNER.name().equalsIgnoreCase(currentUserRoleName)
        && (currentUserShopId == null || !currentUserShopId.equals(existingInvoice.getShopId()))) {
      throw new InvalidRequestException(
          "You do not have permission to cancel invoice of another shop");
    }

    // 4. Staff can only cancel invoices that they created
    if (Roles.STAFF.name().equalsIgnoreCase(currentUserRoleName)
        && (currentUserId == null
            || !cancelInvoiceMapper.isInvoiceCreatedBy(request.getInvoiceId(), currentUserId))) {
      throw new InvalidRequestException("You do not have permission to cancel this invoice");
    }

    // 5. Only allow cancelling serving invoices (status == 0)
    if (!"0".equals(existingInvoice.getStatus())) {
      throw new InvalidRequestException(
          "Cannot cancel an invoice that is already completed or cancelled");
    }

    // 6. Update status to canceled (status == 2)
    int rowsAffected =
        cancelInvoiceMapper.cancelInvoice(
            request.getInvoiceId(), currentUserRoleName, currentUserShopId);

    if (rowsAffected == 0) {
      throw new DataNotFoundException("Invoice cannot be cancelled", request.getInvoiceId());
    }
  }
}
