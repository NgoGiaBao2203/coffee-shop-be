package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.SearchCategoriesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.CategoryResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetCategoriesMapper;
import coffee.api.services.services_interface.category.IGetCategoryService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCategoryServiceImpl implements IGetCategoryService {

  private final GetCategoriesMapper getCategoriesMapper;

  @Override
  public PageResponse<CategoryResult> process(
      SearchCategoriesRequest request, String currentUserRoleName, UUID currentUserShopId) {

    // Manager or Staff must have an assigned shop branch
    if (!Roles.OWNER.getValue().equals(currentUserRoleName) && currentUserShopId == null) {
      throw new InvalidRequestException("User is not assigned to any shop branch");
    }

    long totalElements =
        getCategoriesMapper.countCategoriesFiltered(
            request.trimmedSearch(), currentUserRoleName, currentUserShopId, request.getShopId());

    List<CategoryResult> categories =
        getCategoriesMapper.getCategoriesFiltered(
            request.trimmedSearch(),
            request.getSortBy(),
            request.getSortDirection() != null ? request.getSortDirection().toString() : "ASC",
            request.getSize(),
            request.calcOffset(),
            currentUserRoleName,
            currentUserShopId,
            request.getShopId());

    if (categories == null) {
      categories = Collections.emptyList();
    }

    PaginationMeta pagination =
        PaginationMeta.builder()
            .page(request.getPage())
            .size(request.getSize())
            .totalElements(totalElements)
            .totalPages(request.totalPages(totalElements))
            .build();

    return PageResponse.of("Get categories successfully", categories, pagination);
  }
}
