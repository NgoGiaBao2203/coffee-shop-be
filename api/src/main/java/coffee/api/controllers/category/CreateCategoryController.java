package coffee.api.controllers.category;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import coffee.api.dto.response.category.CreateCategoriesResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.category.ICreateCategoriesService;
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
public class CreateCategoryController {
  private final ICreateCategoriesService createCategoriesService;

  @PostMapping("category/create")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<CreateCategoriesResponse> createCategories(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CreateCategoriesRequest request) {
    createCategoriesService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(CreateCategoriesResponse.of(ResponseCode.SUCCESS, "Category created successfully"));
  }
}
