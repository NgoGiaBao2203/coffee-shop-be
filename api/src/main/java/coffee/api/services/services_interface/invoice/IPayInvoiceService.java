package coffee.api.services.services_interface.invoice;

import coffee.api.dto.request.invoice.PayInvoiceRequest;
import java.util.UUID;

public interface IPayInvoiceService {

  void process(
      PayInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
