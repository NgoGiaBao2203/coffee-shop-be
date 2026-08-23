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
      PayInvoiceRequest request, String currentUserRoleName, UUID currentUserShopId) {

    // Check if invoice exists
    InvoiceResult existingInvoice = payInvoiceMapper.findInvoiceById(request.getInvoiceId());
    if (existingInvoice == null) {
      throw new DataNotFoundException("Invoice not found", request.getInvoiceId());
    }

    // Check shop permission for STAFF & MANAGER
    if (!Roles.OWNER.name().equalsIgnoreCase(currentUserRoleName)) {
      if (currentUserShopId == null || !currentUserShopId.equals(existingInvoice.getShopId())) {
        throw new InvalidRequestException(
            "You do not have permission to pay invoice of another shop");
      }
    }

    // Validate invoice state: only allow paying serving orders (status == 0)
    if (!"0".equals(existingInvoice.getStatus())) {
      throw new InvalidRequestException(
          "Can not pay an invoice that is already completed or cancelled");
    }

    // Update status to 1
    int rowsAffected =
        payInvoiceMapper.payInvoice(request.getInvoiceId(), currentUserRoleName, currentUserShopId);

    if (rowsAffected == 0) {
      throw new DataNotFoundException("Invoice cannot be paid", request.getInvoiceId());
    }
  }
}
