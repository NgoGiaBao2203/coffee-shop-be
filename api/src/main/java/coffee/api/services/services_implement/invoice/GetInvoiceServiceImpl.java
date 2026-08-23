package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.SearchInvoiceRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.mapper.GetInvoiceMapper;
import coffee.api.services.services_interface.invoice.IGetInvoiceService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetInvoiceServiceImpl implements IGetInvoiceService {

  private final GetInvoiceMapper getInvoiceMapper;

  @Override
  public PageResponse<InvoiceResult> process(
      SearchInvoiceRequest request, String currentUserRoleName, UUID currentUserShopId) {

    long totalElements =
        getInvoiceMapper.countInvoicesFiltered(
            request.trimmedSearch(),
            currentUserRoleName,
            currentUserShopId,
            request.getShopId(),
            request.getStatus());

    List<InvoiceResult> invoices =
        getInvoiceMapper.getInvoicesFiltered(
            request.trimmedSearch(),
            request.getSortBy(),
            request.getSortDirection().toString(),
            request.getSize(),
            request.calcOffset(),
            currentUserRoleName,
            currentUserShopId,
            request.getShopId(),
            request.getStatus());

    if (invoices == null) {
      invoices = Collections.emptyList();
    } else {
      invoices.forEach(invoice -> invoice.setStatus(normalizeStatus(invoice.getStatus())));
    }

    PaginationMeta pagination =
        PaginationMeta.builder()
            .page(request.getPage())
            .size(request.getSize())
            .totalElements(totalElements)
            .totalPages(request.totalPages(totalElements))
            .build();

    return PageResponse.of("Get invoices successfully", invoices, pagination);
  }

  private String normalizeStatus(String rawStatus) {
    if (rawStatus == null) return "Không xác định";

    return switch (rawStatus) {
      case "0", "PENDING" -> "Đang phục vụ";
      case "1", "COMPLETED" -> "Đã thanh toán";
      case "2", "CANCELLED" -> "Đã hủy";
      default -> "Không xác định";
    };
  }
}
