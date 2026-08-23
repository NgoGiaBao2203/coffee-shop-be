package coffee.api.services.services_interface.invoice;

import coffee.api.dto.request.invoice.SearchInvoiceRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.InvoiceResult;
import java.util.UUID;

public interface IGetInvoiceService {

  PageResponse<InvoiceResult> process(
      SearchInvoiceRequest request, String currentUserRoleName, UUID currentUserShopId);
}
