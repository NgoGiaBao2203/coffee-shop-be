package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetDrinksMapper;
import coffee.api.services.services_interface.drink.IGetDrinkService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetDrinkServiceImpl implements IGetDrinkService {

  private final GetDrinksMapper getDrinksMapper;

  @Override
  public PageResponse<DrinkResult> process(
      SearchDrinksRequest request, String currentUserRoleName, UUID currentUserShopId) {

    // 1. Manager or Staff must have an assigned shop
    verifyShopBranchAssignment(currentUserRoleName, currentUserShopId);

    // 2. Count total elements matching filter criteria
    long totalElements = countDrinks(request, currentUserRoleName, currentUserShopId);

    // 3. Fetch paginated drinks list
    List<DrinkResult> drinks = fetchDrinks(request, currentUserRoleName, currentUserShopId);

    // 4. Normalize status label for each drink
    normalizeDrinkStatuses(drinks);

    // 5. Build pagination metadata
    PaginationMeta pagination = buildPaginationMeta(request, totalElements);

    return PageResponse.of("Get drinks successfully", drinks, pagination);
  }

  // 1. Manager or Staff must have an assigned shop
  private void verifyShopBranchAssignment(String currentUserRoleName, UUID currentUserShopId) {
    if (!Roles.OWNER.getValue().equals(currentUserRoleName) && currentUserShopId == null) {
      throw new InvalidRequestException("User is not assigned to any shop");
    }
  }

  // 2. Count total elements matching filter criteria
  private long countDrinks(
      SearchDrinksRequest request, String currentUserRoleName, UUID currentUserShopId) {
    return getDrinksMapper.countDrinksFiltered(
        request.trimmedSearch(), currentUserRoleName, currentUserShopId, request.getShopId());
  }

  // 3. Fetch paginated drinks list
  private List<DrinkResult> fetchDrinks(
      SearchDrinksRequest request, String currentUserRoleName, UUID currentUserShopId) {

    String sortDirection =
        request.getSortDirection() != null ? request.getSortDirection().name() : "ASC";

    List<DrinkResult> drinks =
        getDrinksMapper.getDrinksFiltered(
            request.trimmedSearch(),
            request.getSortBy(),
            sortDirection,
            request.getSize(),
            request.calcOffset(),
            currentUserRoleName,
            currentUserShopId,
            request.getShopId());

    return drinks != null ? drinks : Collections.emptyList();
  }

  // 4. Normalize status label for each drink
  private void normalizeDrinkStatuses(List<DrinkResult> drinks) {
    drinks.forEach(drink -> drink.setStatus(normalizeStatus(drink.getStatus())));
  }

  // 5. Build pagination metadata
  private PaginationMeta buildPaginationMeta(SearchDrinksRequest request, long totalElements) {
    return PaginationMeta.builder()
        .page(request.getPage())
        .size(request.getSize())
        .totalElements(totalElements)
        .totalPages(request.totalPages(totalElements))
        .build();
  }

  private String normalizeStatus(String rawStatus) {
    if (rawStatus == null) return "UNKNOWN";

    return switch (rawStatus) {
      case "1", "ACTIVE" -> "Đang bán";
      case "0", "INACTIVE" -> "Ngừng bán";
      default -> "Không xác định";
    };
  }
}
