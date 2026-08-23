package coffee.api.services.services_interface.invoice;

import coffee.api.dto.request.invoice.EditInvoiceRequest;
import java.util.UUID;

public interface IEditInvoiceService {

  void process(EditInvoiceRequest request, String currentUserRoleName, UUID currentUserShopId);
}
