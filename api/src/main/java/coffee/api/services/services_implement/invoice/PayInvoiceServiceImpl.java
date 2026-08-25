package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.PayInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.PayInvoiceMapper;
import coffee.api.services.services_interface.invoice.IPayInvoiceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayInvoiceServiceImpl implements IPayInvoiceService {

  private final PayInvoiceMapper payInvoiceMapper;

  @Override
  @Transactional
  public void process(
      PayInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Check invoice exists
    InvoiceResult existingInvoice = payInvoiceMapper.findInvoiceById(request.getInvoiceId());
    if (existingInvoice == null) {
      throw new DataNotFoundException("Invoice not found", request.getInvoiceId());
    }

    // 2. Only OWNER, MANAGER, STAFF can pay
    if (!Roles.OWNER.name().equalsIgnoreCase(currentUserRoleName)
        && !Roles.MANAGER.name().equalsIgnoreCase(currentUserRoleName)
        && !Roles.STAFF.name().equalsIgnoreCase(currentUserRoleName)) {
      throw new InvalidRequestException("You do not have permission to pay this invoice");
    }

    // 3. Non-owner can only pay invoices from their own shop
    if (!Roles.OWNER.name().equalsIgnoreCase(currentUserRoleName)
        && (currentUserShopId == null || !currentUserShopId.equals(existingInvoice.getShopId()))) {
      throw new InvalidRequestException(
          "You do not have permission to pay invoice of another shop");
    }

    // 4. Staff can only pay invoices that they created
    if (Roles.STAFF.name().equalsIgnoreCase(currentUserRoleName)
        && (currentUserId == null
            || !payInvoiceMapper.isInvoiceCreatedBy(request.getInvoiceId(), currentUserId))) {
      throw new InvalidRequestException("You do not have permission to pay this invoice");
    }

    // 5. Only allow paying serving invoices (status == 0)
    if (!"0".equals(existingInvoice.getStatus())) {
      throw new InvalidRequestException(
          "Can not pay an invoice that is already completed or cancelled");
    }

    // 6. Update status to paid (status == 1)
    int rowsAffected =
        payInvoiceMapper.payInvoice(request.getInvoiceId(), currentUserRoleName, currentUserShopId);

    if (rowsAffected == 0) {
      throw new DataNotFoundException("Invoice cannot be paid", request.getInvoiceId());
    }
  }
}
