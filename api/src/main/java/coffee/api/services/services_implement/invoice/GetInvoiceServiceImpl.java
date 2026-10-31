package coffee.api.services.services_implement.invoice;

import coffee.api.dto.request.invoice.SearchInvoiceRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetInvoiceMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import coffee.api.services.services_interface.invoice.IGetInvoiceService;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetInvoiceServiceImpl implements IGetInvoiceService {

  private final GetInvoiceMapper getInvoiceMapper;
  private final InvoiceValidator invoiceValidator;

  @Override
  public PageResponse<InvoiceResult> process(
      SearchInvoiceRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Verify shop permission
    UUID targetShopId =
        resolveAndValidateShopId(
            request.getShopId(), currentUserRoleName, currentUserShopId, currentUserId);
    String roleName = currentUserRoleName.toUpperCase(Locale.ROOT);

    // 2. Count invoices matching shop, status and search filters
    long totalElements =
        getInvoiceMapper.countInvoicesFiltered(
            request.trimmedSearch(),
            roleName,
            currentUserShopId,
            targetShopId,
            request.getStatus());

    // 3. Get invoice details and normalize status
    List<InvoiceResult> invoices =
        fetchInvoices(request, roleName, currentUserShopId, targetShopId, totalElements);

    // 4. Return invoice list and pagination
    return PageResponse.of(
        "Get invoices successfully", invoices, buildPagination(request, totalElements));
  }

  /** Allow OWNER to filter any shop and restrict MANAGER or STAFF to their assigned shop. */
  private UUID resolveAndValidateShopId(
      UUID requestedShopId,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {
    boolean isOwner = "OWNER".equalsIgnoreCase(currentUserRoleName);
    UUID targetShopId = requestedShopId;
    if (!isOwner) {
      if (!"MANAGER".equalsIgnoreCase(currentUserRoleName)
          && !"STAFF".equalsIgnoreCase(currentUserRoleName)) {
        throw new InvalidRequestException("You do not have permission to access invoices");
      }
      if (currentUserShopId == null) {
        throw new InvalidRequestException("User is not assigned to any shop");
      }
      targetShopId =
          invoiceValidator.resolveTargetShopId(
              currentUserShopId, currentUserRoleName, currentUserShopId);
    }
    if (targetShopId != null) {
      invoiceValidator.validateShopPermission(targetShopId, currentUserRoleName, currentUserId);
    }
    return targetShopId;
  }

  /** Get invoice details only when matching invoices exist and normalize response status. */
  private List<InvoiceResult> fetchInvoices(
      SearchInvoiceRequest request,
      String roleName,
      UUID currentUserShopId,
      UUID targetShopId,
      long totalElements) {
    List<InvoiceResult> invoices = Collections.emptyList();
    if (totalElements > 0) {
      invoices =
          getInvoiceMapper.getInvoicesFiltered(
              request.trimmedSearch(),
              request.getSortBy(),
              request.getSortDirection().toString(),
              request.getSize(),
              request.calcOffset(),
              roleName,
              currentUserShopId,
              targetShopId,
              request.getStatus());
      if (invoices == null) {
        invoices = Collections.emptyList();
      } else {
        invoices.forEach(invoice -> invoice.setStatus(normalizeStatus(invoice.getStatus())));
      }
    }

    return invoices;
  }

  /** Build pagination from request and total matching invoices. */
  private PaginationMeta buildPagination(SearchInvoiceRequest request, long totalElements) {
    return PaginationMeta.builder()
        .page(request.getPage())
        .size(request.getSize())
        .totalElements(totalElements)
        .totalPages(request.totalPages(totalElements))
        .build();
  }

  /** Convert stored invoice status codes into the response status names. */
  private String normalizeStatus(String rawStatus) {
    if (rawStatus == null) return "Không xác định";

    return switch (rawStatus) {
      case "0" -> "PENDING";
      case "1" -> "COMPLETED";
      case "2" -> "CANCELLED";
      default -> "UNDEFINED";
    };
  }
}
