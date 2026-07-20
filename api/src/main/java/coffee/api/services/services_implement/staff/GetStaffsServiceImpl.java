package coffee.api.services.services_implement.staff;

import coffee.api.dto.request.user.SearchUsersRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.ProfileResult;
import coffee.api.repository.staff.GetStaffsRepository;
import coffee.api.services.services_interface.staff.IGetStaffsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetStaffsServiceImpl implements IGetStaffsService {
  private final GetStaffsRepository getStaffsRepository;

  @Override
  public PageResponse<ProfileResult> process(
    SearchUsersRequest request,
    String currentUserRoleName,
    UUID currentUserId
  ) {
    String search = request.trimmedSearch();
    String sortBy = request.getSortBy();
    String sortDirection = request.directionValue();
    int size = request.getSize();
    int offset = request.calcOffset();
    long totalElements = getStaffsRepository.countStaffsFiltered(
      search,
      currentUserRoleName,
      currentUserId
    );
    PaginationMeta pagination = PaginationMeta.builder()
      .page(request.getPage())
      .size(size)
      .totalElements(totalElements)
      .totalPages(request.totalPages(totalElements))
      .build();
    List<ProfileResult> items = getStaffsRepository.getStaffsFiltered(
      search,
      sortBy,
      sortDirection,
      size,
      offset,
      currentUserRoleName,
      currentUserId
    );
    return PageResponse.of("Get staff list successfully", items, pagination);
  }
}
