package coffee.api.services.services_implement.invoice;

import coffee.api.constants.ErrorMessage;
import coffee.api.dto.request.invoice.PrintInvoiceRequest;
import coffee.api.dto.result.InvoicePrintResult;
import coffee.api.dto.result.InvoiceReceiptResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.PrintInvoiceMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceReceiptPdfWriter;
import coffee.api.services.services_implement.invoice.common.InvoiceStatusChanger;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import coffee.api.services.services_interface.invoice.IPrintInvoiceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrintInvoiceServiceImpl implements IPrintInvoiceService {

  private final PrintInvoiceMapper printInvoiceMapper;
  private final InvoiceValidator invoiceValidator;
  private final InvoiceReceiptPdfWriter invoiceReceiptPdfWriter;

  @Override
  public InvoicePrintResult process(
      PrintInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Load the invoice with shop header and drink lines
    InvoiceReceiptResult receipt = printInvoiceMapper.findInvoiceForPrint(request.getInvoiceId());
    if (receipt == null) {
      throw new DataNotFoundException(ErrorMessage.INVOICE_NOT_FOUND, request.getInvoiceId());
    }

    // 2. Shop rules only: any MANAGER or STAFF of the shop may print, not just the creator
    UUID shopId =
        invoiceValidator.resolveTargetShopId(
            receipt.getShopId(), currentUserRoleName, currentUserShopId);
    invoiceValidator.validateShopPermission(shopId, currentUserRoleName, currentUserId);

    // 3. Only a paid invoice gets a receipt
    if (!Integer.valueOf(InvoiceStatusChanger.STATUS_PAID).equals(receipt.getStatus())) {
      throw new InvalidRequestException("Only paid invoices can be printed");
    }

    // 4. Render the 80mm receipt
    return new InvoicePrintResult(
        "hoa-don_" + receipt.getInvoiceCode() + ".pdf", invoiceReceiptPdfWriter.write(receipt));
  }
}
