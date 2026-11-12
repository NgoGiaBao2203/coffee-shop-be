package coffee.api.services.services_implement.revenue;

import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.response.revenue.GetRevenueResponse;
import coffee.api.dto.result.RevenueResult;
import coffee.api.enums.ResponseCode;
import coffee.api.mapper.GetRevenueMapper;
import coffee.api.services.services_implement.revenue.common.RevenueFilterResolver;
import coffee.api.services.services_interface.revenue.IGetRevenueService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRevenueServiceImpl implements IGetRevenueService {

  private final GetRevenueMapper getRevenueMapper;
  private final RevenueFilterResolver revenueFilterResolver;

  @Override
  public GetRevenueResponse process(
      SearchRevenueRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Verify role, shop permission and date range
    UUID targetShopId =
        revenueFilterResolver.resolveTargetShopId(
            request.getShopId(), currentUserRoleName, currentUserShopId, currentUserId);
    revenueFilterResolver.validateDateRange(request.getStartDate(), request.getEndDate());

    // 2. Count paid invoices matching shop, date and search filters
    long totalElements =
        getRevenueMapper.countRevenueFiltered(
            request.trimmedSearch(),
            targetShopId,
            request.getInvoiceId(),
            toStartDateTime(request),
            toEndDateTime(request),
            currentUserRoleName,
            currentUserShopId);

    // 3. Get paid invoices with their drinks for the requested page
    List<RevenueResult> revenues =
        fetchRevenues(request, currentUserRoleName, currentUserShopId, targetShopId, totalElements);

    // 4. Get total revenue of all pages
    BigDecimal totalRevenue =
        fetchTotalRevenue(
            request, currentUserRoleName, currentUserShopId, targetShopId, totalElements);

    // 5. Return revenue invoices, total revenue and pagination
    String message =
        revenues.isEmpty() ? "No revenue records found" : "Get revenue statistics successfully";

    return GetRevenueResponse.of(
        ResponseCode.SUCCESS,
        message,
        totalRevenue,
        revenues,
        buildPagination(request, totalElements));
  }

  /** Get paid invoices only when matching invoices exist. */
  private List<RevenueResult> fetchRevenues(
      SearchRevenueRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID targetShopId,
      long totalElements) {
    if (totalElements == 0) {
      return Collections.emptyList();
    }
    List<RevenueResult> revenues =
        getRevenueMapper.getRevenueFiltered(
            request.trimmedSearch(),
            targetShopId,
            request.getInvoiceId(),
            toStartDateTime(request),
            toEndDateTime(request),
            request.getSortBy(),
            request.directionValue(),
            request.getSize(),
            request.calcOffset(),
            currentUserRoleName,
            currentUserShopId);
    return revenues != null ? revenues : Collections.emptyList();
  }

  /** Get the total revenue only when matching invoices exist; a missing value becomes zero. */
  private BigDecimal fetchTotalRevenue(
      SearchRevenueRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID targetShopId,
      long totalElements) {
    if (totalElements == 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal total =
        getRevenueMapper.getRevenueTotal(
            request.trimmedSearch(),
            targetShopId,
            request.getInvoiceId(),
            toStartDateTime(request),
            toEndDateTime(request),
            currentUserRoleName,
            currentUserShopId);
    return total != null ? total : BigDecimal.ZERO;
  }

  /** Build pagination from request and total matching invoices. */
  private PaginationMeta buildPagination(SearchRevenueRequest request, long totalElements) {
    return PaginationMeta.builder()
        .page(request.getPage())
        .size(request.getSize())
        .totalElements(totalElements)
        .totalPages(request.totalPages(totalElements))
        .build();
  }

  private LocalDateTime toStartDateTime(SearchRevenueRequest request) {
    return revenueFilterResolver.toStartDateTime(request.getStartDate());
  }

  private LocalDateTime toEndDateTime(SearchRevenueRequest request) {
    return revenueFilterResolver.toEndDateTime(request.getEndDate());
  }
}
