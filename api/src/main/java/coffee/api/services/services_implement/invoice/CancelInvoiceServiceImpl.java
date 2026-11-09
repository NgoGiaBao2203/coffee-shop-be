package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.CancelInvoiceRequest;
import coffee.api.services.services_implement.invoice.common.InvoiceStatusChanger;
import coffee.api.services.services_interface.invoice.ICancelInvoiceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelInvoiceServiceImpl implements ICancelInvoiceService {
  private final InvoiceStatusChanger invoiceStatusChanger;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      CancelInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {
    // The target status (cancelled) is decided by the server, never sent by the client
    invoiceStatusChanger.change(
        request.getInvoiceId(),
        InvoiceStatusChanger.STATUS_CANCELLED,
        currentUserRoleName,
        currentUserShopId,
        currentUserId);
  }
}
