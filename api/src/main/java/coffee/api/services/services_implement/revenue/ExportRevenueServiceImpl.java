package coffee.api.services.services_implement.revenue;

import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.result.RevenueExportResult;
import coffee.api.dto.result.RevenueResult;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.ExportRevenueMapper;
import coffee.api.mapper.GetRevenueMapper;
import coffee.api.services.services_implement.revenue.common.RevenueExcelWriter;
import coffee.api.services.services_implement.revenue.common.RevenueFilterResolver;
import coffee.api.services.services_interface.revenue.IExportRevenueService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportRevenueServiceImpl implements IExportRevenueService {

  /** Upper bound of invoices in one file, to keep memory and file size reasonable. */
  static final int MAX_EXPORT_INVOICES = 20_000;

  private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

  private final ExportRevenueMapper exportRevenueMapper;
  private final GetRevenueMapper getRevenueMapper;
  private final RevenueFilterResolver revenueFilterResolver;
  private final RevenueExcelWriter revenueExcelWriter;

  @Override
  public RevenueExportResult process(
      SearchRevenueRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {

    // 1. Same role, shop and date rules as the revenue list
    UUID targetShopId =
        revenueFilterResolver.resolveTargetShopId(
            request.getShopId(), currentUserRoleName, currentUserShopId, currentUserId);
    revenueFilterResolver.validateDateRange(request.getStartDate(), request.getEndDate());
    LocalDateTime startDateTime = revenueFilterResolver.toStartDateTime(request.getStartDate());
    LocalDateTime endDateTime = revenueFilterResolver.toEndDateTime(request.getEndDate());

    // 2. Reject exports that are too large before loading them (same count as the revenue list)
    long totalInvoices =
        getRevenueMapper.countRevenueFiltered(
            request.trimmedSearch(),
            targetShopId,
            request.getInvoiceId(),
            startDateTime,
            endDateTime,
            currentUserRoleName,
            currentUserShopId);
    if (totalInvoices > MAX_EXPORT_INVOICES) {
      throw new InvalidRequestException(
          "Too many invoices to export (maximum "
              + MAX_EXPORT_INVOICES
              + "). Please narrow the filter");
    }

    // 3. Load every matching paid invoice with its drinks (no pagination)
    List<RevenueResult> invoices = Collections.emptyList();
    if (totalInvoices > 0) {
      List<RevenueResult> rows =
          exportRevenueMapper.getRevenueForExport(
              request.trimmedSearch(),
              targetShopId,
              request.getInvoiceId(),
              startDateTime,
              endDateTime,
              request.getSortBy(),
              request.directionValue(),
              currentUserRoleName,
              currentUserShopId);
      invoices = rows != null ? rows : Collections.emptyList();
    }

    // 4. Build the workbook
    String shopName = targetShopId != null ? exportRevenueMapper.getShopName(targetShopId) : null;
    String preparedBy =
        currentUserId != null ? exportRevenueMapper.getProfileName(currentUserId) : null;
    byte[] content =
        revenueExcelWriter.write(
            invoices,
            new RevenueExcelWriter.ReportInfo(
                request.getStartDate(),
                request.getEndDate(),
                shopName,
                request.trimmedSearch(),
                preparedBy));

    return new RevenueExportResult(buildFileName(request), content);
  }

  /**
   * e.g. doanh-thu_20261101_20261130.xlsx; no dates = doanh-thu_tat-ca.xlsx; one open bound becomes
   * "batdau" / "hientai".
   */
  private String buildFileName(SearchRevenueRequest request) {
    if (request.getStartDate() == null && request.getEndDate() == null) {
      return "doanh-thu_tat-ca.xlsx";
    }
    return "doanh-thu_"
        + formatOr(request.getStartDate(), "batdau")
        + "_"
        + formatOr(request.getEndDate(), "hientai")
        + ".xlsx";
  }

  private String formatOr(LocalDate date, String fallback) {
    return date != null ? date.format(FILE_DATE) : fallback;
  }
}
