package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.PayInvoiceRequest;
import coffee.api.services.services_implement.invoice.common.InvoiceStatusChanger;
import coffee.api.services.services_interface.invoice.IPayInvoiceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayInvoiceServiceImpl implements IPayInvoiceService {
  private final InvoiceStatusChanger invoiceStatusChanger;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      PayInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {
    // The target status (paid) is decided by the server, never sent by the client
    invoiceStatusChanger.change(
        request.getInvoiceId(),
        InvoiceStatusChanger.STATUS_PAID,
        currentUserRoleName,
        currentUserShopId,
        currentUserId);
  }
}
