package coffee.api.services.services_interface.invoice;

import coffee.api.dto.request.invoice.PrintInvoiceRequest;
import coffee.api.dto.result.InvoicePrintResult;
import java.util.UUID;

public interface IPrintInvoiceService {

  InvoicePrintResult process(
      PrintInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
