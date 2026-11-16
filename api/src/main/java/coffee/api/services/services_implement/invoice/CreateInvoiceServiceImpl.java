package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.CreateInvoiceRequest;
import coffee.api.dto.response.invoice.CreateInvoiceResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import coffee.api.services.services_interface.invoice.ICreateInvoiceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateInvoiceServiceImpl implements ICreateInvoiceService {

  private static final int STATUS_UNPAID = 0;

  private static final DateTimeFormatter INVOICE_CODE_DATE =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  private final CreateInvoiceMapper createInvoiceMapper;
  private final InvoiceValidator invoiceValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CreateInvoiceResponse process(
      CreateInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. The server always creates an invoice as unpaid; the client cannot choose the status

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
        invoiceId,
        generateInvoiceCode(invoiceId),
        targetShopId,
        currentUserId,
        tableId,
        totalAmount,
        STATUS_UNPAID);

    createInvoiceMapper.insertInvoiceDetails(invoiceId, request.getItems());

    return CreateInvoiceResponse.of(ResponseCode.SUCCESS, "Create invoice successfully", invoiceId);
  }

  /** Build a code like INV-20260101-1A2B3C4D from the creation date and the invoice id. */
  private String generateInvoiceCode(UUID invoiceId) {
    String datePart = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(INVOICE_CODE_DATE);
    String suffix = invoiceId.toString().replace("-", "").substring(0, 8).toUpperCase();
    return "INV-" + datePart + "-" + suffix;
  }
}
