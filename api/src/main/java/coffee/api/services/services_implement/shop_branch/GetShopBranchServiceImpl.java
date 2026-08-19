package coffee.api.services.services_implement.shop_branch;

import coffee.api.dto.request.shop_branch.SearchShopBranchRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.ShopBranchResult;
import coffee.api.mapper.GetShopBranchMapper;
import coffee.api.services.services_interface.shop_branch.IGetShopBranchService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetShopBranchServiceImpl implements IGetShopBranchService {

  private final GetShopBranchMapper getShopBranchMapper;

  @Override
  public PageResponse<ShopBranchResult> process(
      SearchShopBranchRequest request, String currentUserRoleName, UUID currentUserId) {

    long totalElements =
        getShopBranchMapper.countShopBranchesFiltered(
            request.trimmedSearch(), currentUserRoleName, currentUserId);

    List<ShopBranchResult> branches =
        getShopBranchMapper.getShopBranchesFiltered(
            request.trimmedSearch(),
            request.getSortBy(),
            request.getSortDirection() != null ? request.getSortDirection().toString() : "ASC",
            request.getSize(),
            request.calcOffset(),
            currentUserRoleName,
            currentUserId);

    if (branches == null) {
      branches = Collections.emptyList();
    }

    PaginationMeta pagination =
        PaginationMeta.builder()
            .page(request.getPage())
            .size(request.getSize())
            .totalElements(totalElements)
            .totalPages(request.totalPages(totalElements))
            .build();

    return PageResponse.of("Get shop branches successfully", branches, pagination);
  }
}
