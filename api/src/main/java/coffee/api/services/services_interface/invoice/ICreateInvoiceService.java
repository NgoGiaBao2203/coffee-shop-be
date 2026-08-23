package coffee.api.services.services_interface.invoice;

import coffee.api.dto.request.invoice.CreateInvoiceRequest;
import coffee.api.dto.response.invoice.CreateInvoiceResponse;
import java.util.UUID;

public interface ICreateInvoiceService {

  CreateInvoiceResponse process(
      CreateInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentProfileId);
}
