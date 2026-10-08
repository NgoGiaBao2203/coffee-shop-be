package coffee.api.services.services_implement.shop;

import coffee.api.dto.request.shop.SearchShopRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.ShopResult;
import coffee.api.mapper.GetShopMapper;
import coffee.api.services.services_interface.shop.IGetShopService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetShopServiceImpl implements IGetShopService {

  private final GetShopMapper getShopMapper;

  @Override
  public PageResponse<ShopResult> process(
      SearchShopRequest request, String currentUserRoleName, UUID currentUserId) {

    long totalElements =
        getShopMapper.countShopsFiltered(
            request.trimmedSearch(), currentUserRoleName, currentUserId);

    List<ShopResult> shops =
        getShopMapper.getShopsFiltered(
            request.trimmedSearch(),
            request.getSortBy(),
            request.getSortDirection() != null ? request.getSortDirection().toString() : "ASC",
            request.getSize(),
            request.calcOffset(),
            currentUserRoleName,
            currentUserId);

    if (shops == null) {
      shops = Collections.emptyList();
    }

    PaginationMeta pagination =
        PaginationMeta.builder()
            .page(request.getPage())
            .size(request.getSize())
            .totalElements(totalElements)
            .totalPages(request.totalPages(totalElements))
            .build();

    return PageResponse.of("Get shops successfully", shops, pagination);
  }
}
