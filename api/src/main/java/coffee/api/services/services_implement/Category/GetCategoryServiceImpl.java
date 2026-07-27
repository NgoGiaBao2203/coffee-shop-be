package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.SearchCategoriesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.CategoryResult;
import coffee.api.mapper.GetCategoriesMapper;
import coffee.api.services.services_interface.category.IGetCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCategoryServiceImpl implements IGetCategoryService {

  private final GetCategoriesMapper getCategoriesMapper;

  @Override
  public PageResponse<CategoryResult> process(
    SearchCategoriesRequest request,
    String currentUserRoleName,
    UUID currentUserShopId
  ) {
    long totalElements = getCategoriesMapper.countCategoriesFiltered(
      request.trimmedSearch(),
      currentUserRoleName,
      currentUserShopId
    );

    List<CategoryResult> categories = getCategoriesMapper.getCategoriesFiltered(
      request.trimmedSearch(),
      request.getSortBy(),
      request.getSortDirection().toString(),
      request.getSize(),
      request.calcOffset(),
      currentUserRoleName,
      currentUserShopId
    );

    if (categories == null) {
      categories = Collections.emptyList();
    }

    PaginationMeta pagination = PaginationMeta.builder()
      .page(request.getPage())
      .size(request.getSize())
      .totalElements(totalElements)
      .totalPages(request.totalPages(totalElements))
      .build();

    return PageResponse.of("Get categories successfully", categories, pagination);
  }
}