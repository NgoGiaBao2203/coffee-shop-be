package coffee.api.services.services_interface.invoice;

import coffee.api.dto.request.invoice.StatusInvoiceRequest;
import java.util.UUID;

public interface IStatusInvoiceService {

  void process(
      StatusInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
