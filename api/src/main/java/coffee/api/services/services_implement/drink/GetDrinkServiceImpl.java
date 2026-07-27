package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.DrinkResult;
import coffee.api.mapper.GetDrinksMapper;
import coffee.api.services.services_interface.drink.IGetDrinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetDrinkServiceImpl implements IGetDrinkService {
  private final GetDrinksMapper getDrinksMapper;

  @Override
  public PageResponse<DrinkResult> process(
    SearchDrinksRequest request,
    String currentUserRoleName,
    UUID currentUserShopId
  ) {
    long totalElements = getDrinksMapper.countDrinksFiltered(
      request.trimmedSearch(),
      currentUserShopId,
      currentUserRoleName
    );

    List<DrinkResult> drinks = getDrinksMapper.getDrinksFiltered(
      request.trimmedSearch(),
      request.getSortBy(),
      request.getSortDirection().toString(),
      request.getSize(),
      request.calcOffset(),
      currentUserShopId,
      currentUserRoleName
    );

    if (drinks == null) {
      drinks = Collections.emptyList();
    }

    PaginationMeta pagination = PaginationMeta.builder()
      .page(request.getPage())
      .size(request.getSize())
      .totalElements(totalElements)
      .totalPages(request.totalPages(totalElements))
      .build();

    return PageResponse.of("Get drinks successfully", drinks, pagination);
  }
}