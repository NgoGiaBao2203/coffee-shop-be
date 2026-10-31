package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.CreateInvoiceRequest;
import coffee.api.dto.response.invoice.CreateInvoiceResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import coffee.api.services.services_interface.invoice.ICreateInvoiceService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateInvoiceServiceImpl implements ICreateInvoiceService {

  private final CreateInvoiceMapper createInvoiceMapper;
  private final InvoiceValidator invoiceValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CreateInvoiceResponse process(
      CreateInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Verify initial invoice status
    Integer status = request.getStatus();
    validateInitialInvoiceStatus(status);

    // 2. Verify shop permission
    UUID targetShopId =
        invoiceValidator.resolveTargetShopId(
            request.getShopId(), currentUserRoleName, currentUserShopId);
    invoiceValidator.validateShopPermission(targetShopId, currentUserRoleName, currentUserId);

    // 3. Verify table belongs to shop and is available table
    UUID tableId = request.getTableId();
    invoiceValidator.validateAvailableTable(tableId, targetShopId);

    // 4. Verify sellable drink details and submitted prices before calculating total
    BigDecimal totalAmount =
        invoiceValidator.validateItemPricesAndCalculateTotal(request.getItems(), targetShopId);

    // 5. Mark the available table as occupied in the same transaction
    invoiceValidator.occupyAvailableTable(tableId, targetShopId);

    // 6. Insert new invoice and invoice detail records
    UUID invoiceId = UUID.randomUUID();

    createInvoiceMapper.insertInvoice(
        invoiceId, targetShopId, currentUserId, tableId, totalAmount, status);

    createInvoiceMapper.insertInvoiceDetails(invoiceId, request.getItems());

    return CreateInvoiceResponse.of(ResponseCode.SUCCESS, "Create invoice successfully");
  }

  /** Only allow unpaid or paid status when creating an invoice. */
  private void validateInitialInvoiceStatus(Integer status) {
    if (status == null || (status != 0 && status != 1)) {
      throw new InvalidRequestException("Invoice status must be 0 (unpaid) or 1 (paid)");
    }
  }
}
