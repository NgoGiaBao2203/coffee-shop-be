package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.EditInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.mapper.UpdateInvoiceMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
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
  private final CreateInvoiceMapper createInvoiceMapper;
  private final InvoiceValidator invoiceValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {
    // 1. Verify shop permission using the same rules as create
    UUID shopId =
        invoiceValidator.resolveTargetShopId(
            request.getShopId(), currentUserRoleName, currentUserShopId);
    invoiceValidator.validateShopPermission(shopId, currentUserRoleName, currentUserId);

    // 2. Lock the invoice and verify it is unpaid and editable by this user
    InvoiceResult invoice = updateInvoiceMapper.findInvoiceById(request.getInvoiceId());
    if (invoice == null) {
      throw new DataNotFoundException("Invoice not found", request.getInvoiceId());
    }
    validateEditableInvoice(invoice, shopId, currentUserRoleName, currentUserId);

    // 3. Verify the current table and the requested destination
    validateCurrentTable(invoice.getTableId(), shopId);
    boolean changingTable = !invoice.getTableId().equals(request.getTableId());
    if (changingTable) {
      invoiceValidator.validateAvailableTable(request.getTableId(), shopId);
    }

    // 4. Verify sellable drinks and submitted prices before calculating the total
    BigDecimal totalAmount =
        invoiceValidator.validateItemPricesAndCalculateTotal(request.getItems(), shopId);

    // 5. Claim the destination table before moving the invoice
    if (changingTable) {
      invoiceValidator.occupyAvailableTable(request.getTableId(), shopId);
    }

    // 6. Update only an unpaid invoice and replace its details
    if (updateInvoiceMapper.updateInvoice(
            request.getInvoiceId(), request.getTableId(), totalAmount, shopId)
        == 0) {
      throw new InvalidRequestException("Invoice cannot be edited. Please reload and try again");
    }
    updateInvoiceMapper.deleteInvoiceDetails(request.getInvoiceId());
    if (createInvoiceMapper.insertInvoiceDetails(request.getInvoiceId(), request.getItems())
        != request.getItems().size()) {
      throw new InvalidRequestException("Invoice details could not be saved");
    }

    // 7. Release the old table only after the invoice has moved successfully
    if (changingTable && updateInvoiceMapper.releaseTable(invoice.getTableId(), shopId) == 0) {
      throw new InvalidRequestException(
          "Previous table cannot be released. Please reload and try again");
    }
  }

  /** Verify invoice shop, STAFF ownership and unpaid status before editing. */
  private void validateEditableInvoice(
      InvoiceResult invoice, UUID requestedShopId, String roleName, UUID currentUserId) {
    if (!requestedShopId.equals(invoice.getShopId())) {
      throw new InvalidRequestException("Invoice does not belong to this shop");
    }
    if (Roles.STAFF.name().equalsIgnoreCase(roleName)
        && !currentUserId.equals(invoice.getProfileId())) {
      throw new InvalidRequestException("You do not have permission to edit this invoice");
    }
    if (!"0".equals(invoice.getStatus())) {
      throw new InvalidRequestException(
          "Cannot edit an invoice that is already completed or cancelled");
    }
  }

  /** Verify the current table belongs to the shop, is not deleted and is occupied, then lock it. */
  private void validateCurrentTable(UUID tableId, UUID shopId) {
    if (tableId == null || updateInvoiceMapper.lockOccupiedTable(tableId, shopId) == null) {
      throw new InvalidRequestException(
          "Current table is unavailable or does not belong to this shop");
    }
  }
}
