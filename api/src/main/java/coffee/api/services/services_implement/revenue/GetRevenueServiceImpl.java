package coffee.api.services.services_implement.revenue;

import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.RevenueResult;
import coffee.api.mapper.GetRevenueMapper;
import coffee.api.services.services_interface.revenue.IGetRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetRevenueServiceImpl implements IGetRevenueService {

  private final GetRevenueMapper getRevenueMapper;

  @Override
  public PageResponse<RevenueResult> process(
    SearchRevenueRequest request,
    String currentUserRoleName,
    UUID currentUserShopId
  ) {
    LocalDateTime startDateTime = (request.getStartDate() != null)
      ? request.getStartDate().atStartOfDay()
      : null;

    LocalDateTime endDateTime = (request.getEndDate() != null)
      ? request.getEndDate().atTime(LocalTime.MAX)
      : null;

    long totalElements = getRevenueMapper.countRevenueFiltered(
      request.trimmedSearch(),
      request.getShopId(),
      request.getInvoiceId(),
      startDateTime,
      endDateTime,
      request.getYear(),
      request.getMonth(),
      currentUserRoleName,
      currentUserShopId
    );

    List<RevenueResult> revenues = getRevenueMapper.getRevenueFiltered(
      request.trimmedSearch(),
      request.getShopId(),
      request.getInvoiceId(),
      startDateTime,
      endDateTime,
      request.getYear(),
      request.getMonth(),
      request.getSortBy(),
      request.directionValue(),
      request.getSize(),
      request.calcOffset(),
      currentUserRoleName,
      currentUserShopId
    );

    boolean isEmpty = (revenues == null || revenues.isEmpty());
    if (isEmpty) {
      revenues = Collections.emptyList();
    }

    PaginationMeta pagination = PaginationMeta.builder()
      .page(request.getPage())
      .size(request.getSize())
      .totalElements(totalElements)
      .totalPages(request.totalPages(totalElements))
      .build();

    String message = isEmpty
      ? "No revenue records found"
      : "Get revenue statistics successfully";

    return PageResponse.of(message, revenues, pagination);
  }
}