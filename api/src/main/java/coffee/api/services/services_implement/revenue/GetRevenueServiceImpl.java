package coffee.api.services.services_implement.revenue;

import coffee.api.constants.ErrorMessage;
import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.response.revenue.GetRevenueResponse;
import coffee.api.dto.result.RevenueResult;
import coffee.api.dto.result.RevenueTotalResult;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetRevenueMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import coffee.api.services.services_interface.revenue.IGetRevenueService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRevenueServiceImpl implements IGetRevenueService {

  private final GetRevenueMapper getRevenueMapper;
  private final InvoiceValidator invoiceValidator;

  @Override
  public GetRevenueResponse process(
      SearchRevenueRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Verify role, shop permission and date range
    UUID targetShopId =
        resolveTargetShopId(
            request.getShopId(), currentUserRoleName, currentUserShopId, currentUserId);
    validateDateRange(request);

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

    // 4. Get total revenue and invoice count of all pages
    RevenueTotalResult total =
        fetchTotal(request, currentUserRoleName, currentUserShopId, targetShopId, totalElements);

    // 5. Return revenue invoices, total and pagination
    String message =
        revenues.isEmpty() ? "No revenue records found" : "Get revenue statistics successfully";

    return GetRevenueResponse.of(
        ResponseCode.SUCCESS,
        message,
        total.getTotalRevenue(),
        total.getTotalInvoices(),
        revenues,
        buildPagination(request, totalElements));
  }

  /**
   * Allow OWNER to view any shop (or all shops when no shop is requested) and restrict MANAGER to
   * the assigned shop. Other roles are denied. Returns the shop to filter by, or null for all.
   */
  private UUID resolveTargetShopId(
      UUID requestedShopId,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {
    UUID targetShopId = requestedShopId;
    if (!Roles.OWNER.getValue().equals(currentUserRoleName)) {
      if (!Roles.MANAGER.getValue().equals(currentUserRoleName)) {
        throw new InvalidRequestException("You do not have permission to access revenue");
      }
      if (currentUserShopId == null) {
        throw new InvalidRequestException(ErrorMessage.USER_NOT_ASSIGNED_TO_SHOP);
      }
      targetShopId = currentUserShopId;
    }
    if (targetShopId != null) {
      invoiceValidator.validateShopPermission(targetShopId, currentUserRoleName, currentUserId);
    }
    return targetShopId;
  }

  /** Verify the start date is not after the end date. */
  private void validateDateRange(SearchRevenueRequest request) {
    if (request.getStartDate() != null
        && request.getEndDate() != null
        && request.getStartDate().isAfter(request.getEndDate())) {
      throw new InvalidRequestException("startDate must not be after endDate");
    }
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

  /** Get the total only when matching invoices exist; missing values become zero. */
  private RevenueTotalResult fetchTotal(
      SearchRevenueRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID targetShopId,
      long totalElements) {
    RevenueTotalResult total = null;
    if (totalElements > 0) {
      total =
          getRevenueMapper.getRevenueTotal(
              request.trimmedSearch(),
              targetShopId,
              request.getInvoiceId(),
              toStartDateTime(request),
              toEndDateTime(request),
              currentUserRoleName,
              currentUserShopId);
    }
    RevenueTotalResult result = new RevenueTotalResult();
    result.setTotalRevenue(
        total != null && total.getTotalRevenue() != null
            ? total.getTotalRevenue()
            : BigDecimal.ZERO);
    result.setTotalInvoices(
        total != null && total.getTotalInvoices() != null ? total.getTotalInvoices() : 0L);
    return result;
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

  /** Start of the start date, or null when no start date is requested. */
  private LocalDateTime toStartDateTime(SearchRevenueRequest request) {
    return request.getStartDate() != null ? request.getStartDate().atStartOfDay() : null;
  }

  /** End of the end date, or null when no end date is requested. */
  private LocalDateTime toEndDateTime(SearchRevenueRequest request) {
    return request.getEndDate() != null ? request.getEndDate().atTime(LocalTime.MAX) : null;
  }
}
