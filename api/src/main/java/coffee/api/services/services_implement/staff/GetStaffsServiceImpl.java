package coffee.api.services.services_implement.staff;

import coffee.api.dto.request.user.SearchUsersRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.ProfileResult;
import coffee.api.mapper.GetStaffsMapper;
import coffee.api.services.services_interface.staff.IGetStaffsService;
import coffee.api.utils.ConvertRoleVN;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStaffsServiceImpl implements IGetStaffsService {
  private final GetStaffsMapper getStaffsMapper;

  @Override
  public PageResponse<ProfileResult> process(
      SearchUsersRequest request, String currentUserRoleName, UUID currentUserId) {
    String search = request.trimmedSearch();
    String roleId = request.getRoleId();
    String branchShopId = request.getBranchShopId();
    String sortBy = request.getSortBy();
    String sortDirection = request.directionValue();
    int size = request.getSize();
    int offset = request.calcOffset();

    long totalElements =
        getStaffsMapper.countStaffsFiltered(
            search, roleId, branchShopId, currentUserRoleName, currentUserId);

    PaginationMeta pagination =
        PaginationMeta.builder()
            .page(request.getPage())
            .size(size)
            .totalElements(totalElements)
            .totalPages(request.totalPages(totalElements))
            .build();

    List<ProfileResult> rawItems =
        getStaffsMapper.getStaffsFiltered(
            search,
            roleId,
            branchShopId,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserId);

    List<ProfileResult> processedItems =
        rawItems.stream()
            .peek(
                profile -> {
                  String vnRole = ConvertRoleVN.toVietnamese(profile.getRoleName());
                  profile.setRoleName(vnRole);
                })
            .collect(Collectors.toList());

    return PageResponse.of("Get staff list successfully", processedItems, pagination);
  }
}
