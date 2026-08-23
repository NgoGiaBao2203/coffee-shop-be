package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.EditInvoiceItemRequest;
import coffee.api.dto.request.invoice.EditInvoiceRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.UpdateInvoiceMapper;
import coffee.api.services.services_interface.invoice.IEditInvoiceService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditInvoiceServiceImpl implements IEditInvoiceService {

  private final UpdateInvoiceMapper updateInvoiceMapper;

  @Override
  @Transactional
  public void process(
      EditInvoiceRequest request, String currentUserRoleName, UUID currentUserShopId) {

    BigDecimal totalAmount = BigDecimal.ZERO;
    for (EditInvoiceItemRequest item : request.getItems()) {
      BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
      totalAmount = totalAmount.add(itemTotal);
    }

    int rowsAffected =
        updateInvoiceMapper.updateInvoice(
            request.getInvoiceId(),
            request.getTableNumber(),
            totalAmount,
            currentUserRoleName,
            currentUserShopId);

    if (rowsAffected == 0) {
      throw new DataNotFoundException(
          "Invoice not found or cannot be edited", request.getInvoiceId());
    }

    updateInvoiceMapper.deleteInvoiceDetails(request.getInvoiceId());
    updateInvoiceMapper.batchInsertInvoiceDetails(request.getInvoiceId(), request.getItems());
  }
}
