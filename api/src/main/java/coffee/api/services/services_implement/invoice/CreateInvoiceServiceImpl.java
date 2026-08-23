package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.CreateInvoiceItemRequest;
import coffee.api.dto.request.invoice.CreateInvoiceRequest;
import coffee.api.dto.response.invoice.CreateInvoiceResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.services.services_interface.invoice.ICreateInvoiceService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateInvoiceServiceImpl implements ICreateInvoiceService {

  private final CreateInvoiceMapper createInvoiceMapper;

  @Override
  @Transactional
  public CreateInvoiceResponse process(
      CreateInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    UUID targetShopId = resolveTargetShopId(request, currentUserRoleName, currentUserShopId);

    BigDecimal totalAmount = BigDecimal.ZERO;
    for (CreateInvoiceItemRequest item : request.getItems()) {
      BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
      totalAmount = totalAmount.add(itemTotal);
    }

    UUID invoiceId = UUID.randomUUID();
    int initialStatus = 0;

    createInvoiceMapper.insertInvoice(
        invoiceId,
        targetShopId,
        currentUserId,
        request.getTableNumber(),
        totalAmount,
        initialStatus);

    createInvoiceMapper.insertInvoiceDetails(invoiceId, request.getItems());

    return CreateInvoiceResponse.of(ResponseCode.SUCCESS, "Create invoice successfully");
  }

  private UUID resolveTargetShopId(
      CreateInvoiceRequest request, String currentUserRoleName, UUID currentUserShopId) {
    if (Roles.OWNER.name().equalsIgnoreCase(currentUserRoleName)) {
      if (request.getShopId() == null) {
        throw new InvalidRequestException("Shop ID is required for OWNER");
      }
      return request.getShopId();
    }
    return currentUserShopId;
  }
}
