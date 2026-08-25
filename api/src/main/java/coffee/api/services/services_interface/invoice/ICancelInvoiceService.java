package coffee.api.services.services_interface.invoice;

import coffee.api.dto.request.invoice.CancelInvoiceRequest;
import java.util.UUID;

public interface ICancelInvoiceService {

  void process(
      CancelInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
