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

    // Manager or Staff must have an assigned shop
    if (!Roles.OWNER.getValue().equals(currentUserRoleName) && currentUserShopId == null) {
      throw new InvalidRequestException("User is not assigned to any shop");
    }

    String sortDirection =
        request.getSortDirection() != null ? request.getSortDirection().name() : "ASC";

    long totalElements =
        getDrinksMapper.countDrinksFiltered(
            request.trimmedSearch(), currentUserRoleName, currentUserShopId, request.getShopId());

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

    if (drinks == null) {
      drinks = Collections.emptyList();
    } else {
      drinks.forEach(drink -> drink.setStatus(normalizeStatus(drink.getStatus())));
    }

    PaginationMeta pagination =
        PaginationMeta.builder()
            .page(request.getPage())
            .size(request.getSize())
            .totalElements(totalElements)
            .totalPages(request.totalPages(totalElements))
            .build();

    return PageResponse.of("Get drinks successfully", drinks, pagination);
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
