package coffee.api.controllers.category;

import coffee.api.dto.request.category.SearchCategoriesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.category.GetCategoryResponse;
import coffee.api.dto.result.CategoryResult;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.category.IGetCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetCategoryController {

  private final IGetCategoryService getCategoryService;

  @PostMapping("categories")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public ResponseEntity<GetCategoryResponse> getCategories(
    @AuthenticationPrincipal CustomUserDetail customUserDetail,
    @RequestBody @Valid SearchCategoriesRequest request
  ) {
    PageResponse<CategoryResult> response = getCategoryService.process(
      request,
      customUserDetail.getRoleName(),
      customUserDetail.getShopId()
    );

    return ResponseEntity.ok()
      .body(GetCategoryResponse.of(
        ResponseCode.SUCCESS,
        "Categories retrieved successfully",
        response
      ));
  }
}