package coffee.api.services.services_interface.category;

import coffee.api.dto.result.CategoryResult;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.request.category.SearchCategoriesRequest;

import java.util.UUID;

public interface IGetCategoryService {
  PageResponse<CategoryResult> process(
    SearchCategoriesRequest request,
    String currentUserRoleName,
    UUID currentUserShopId
  );
}
