package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.EditInvoiceItemRequest;
import coffee.api.dto.request.invoice.EditInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdateInvoiceMapper;
import coffee.api.services.services_interface.invoice.IEditInvoiceService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditInvoiceServiceImpl implements IEditInvoiceService {

  private final UpdateInvoiceMapper updateInvoiceMapper;

  @Override
  @Transactional
  public void process(
      EditInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Check invoice exists
    InvoiceResult existingInvoice = updateInvoiceMapper.findInvoiceById(request.getInvoiceId());
    if (existingInvoice == null) {
      throw new DataNotFoundException("Invoice not found", request.getInvoiceId());
    }

    // 2. Only OWNER, MANAGER, STAFF can edit
    if (!Roles.OWNER.name().equalsIgnoreCase(currentUserRoleName)
        && !Roles.MANAGER.name().equalsIgnoreCase(currentUserRoleName)
        && !Roles.STAFF.name().equalsIgnoreCase(currentUserRoleName)) {
      throw new InvalidRequestException("You do not have permission to edit this invoice");
    }

    // 3. Non-owner can only edit invoices from their own shop
    if (!Roles.OWNER.name().equalsIgnoreCase(currentUserRoleName)
        && (currentUserShopId == null || !currentUserShopId.equals(existingInvoice.getShopId()))) {
      throw new InvalidRequestException(
          "You do not have permission to edit invoice of another shop");
    }

    // 4. Staff can only edit invoices that they created
    if (Roles.STAFF.name().equalsIgnoreCase(currentUserRoleName)
        && (currentUserId == null
            || !updateInvoiceMapper.isInvoiceCreatedBy(request.getInvoiceId(), currentUserId))) {
      throw new InvalidRequestException("You do not have permission to edit this invoice");
    }

    // 5. Only allow editing serving invoices (status == 0)
    if (!"0".equals(existingInvoice.getStatus())) {
      throw new InvalidRequestException(
          "Cannot edit an invoice that is already completed or cancelled");
    }

    // 6. Calculate total money (price * quantity for every item)
    BigDecimal totalAmount = BigDecimal.ZERO;
    for (EditInvoiceItemRequest item : request.getItems()) {
      BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
      totalAmount = totalAmount.add(itemTotal);
    }

    int rowsAffected =
        updateInvoiceMapper.updateInvoice(
            request.getInvoiceId(),
            request.getTableNumber(),
            totalAmount,
            currentUserRoleName,
            currentUserShopId);

    if (rowsAffected == 0) {
      throw new DataNotFoundException(
          "Invoice not found or cannot be edited", request.getInvoiceId());
    }

    updateInvoiceMapper.deleteInvoiceDetails(request.getInvoiceId());
    updateInvoiceMapper.batchInsertInvoiceDetails(request.getInvoiceId(), request.getItems());
  }
}
