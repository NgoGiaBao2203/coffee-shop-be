package coffee.api.services.services_implement.table;

import coffee.api.dto.request.table.SearchTablesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.TableResult;
import coffee.api.mapper.GetTablesMapper;
import coffee.api.services.services_interface.table.IGetTablesService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetTablesServiceImpl implements IGetTablesService {
  private final GetTablesMapper getTablesMapper;

  @Override
  public PageResponse<TableResult> process(
      SearchTablesRequest request, String currentUserRoleName, UUID currentUserId) {
    String search = request.trimmedSearch();
    Integer status = request.getStatus();
    String sortBy = request.getSortBy();
    String sortDirection = request.directionValue();
    int size = request.getSize();
    int offset = request.calcOffset();

    long totalElements =
        getTablesMapper.countTablesFiltered(search, status, currentUserRoleName, currentUserId);

    PaginationMeta pagination =
        PaginationMeta.builder()
            .page(request.getPage())
            .size(size)
            .totalElements(totalElements)
            .totalPages(request.totalPages(totalElements))
            .build();

    List<TableResult> rawItems =
        getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserId);

    List<TableResult> processedItems =
        rawItems.stream()
            .peek(
                table -> {
                  String statusName = getStatusName(table.getStatus());
                  table.setStatusName(statusName);
                })
            .collect(Collectors.toList());

    return PageResponse.of("Get tables successfully", processedItems, pagination);
  }

  private String getStatusName(Integer status) {
    if (status == null) return "Unknown";
    return switch (status) {
      case 1 -> "Available";
      case 2 -> "Occupied";
      case 3 -> "Reserved";
      default -> "Unknown";
    };
  }
}
