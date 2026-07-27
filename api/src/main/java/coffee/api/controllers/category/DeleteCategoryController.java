package coffee.api.controllers.category;

import coffee.api.dto.request.category.DeleteCategoriesRequest;
import coffee.api.dto.response.category.DeleteCategoriesResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.category.IDeleteCategoriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeleteCategoryController {
  private final IDeleteCategoriesService deleteCategoriesService;

  @DeleteMapping("category/delete")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<DeleteCategoriesResponse> deleteCategories(
    @AuthenticationPrincipal CustomUserDetail customUserDetail,
    @RequestBody @Valid DeleteCategoriesRequest request
  ) {
    deleteCategoriesService.process(
      request,
      customUserDetail.getRoleName(),
      customUserDetail.getShopId()
    );
    return ResponseEntity.ok().body(
      DeleteCategoriesResponse.of(
        ResponseCode.SUCCESS,
        "Category deleted successfully"
      )
    );
  }
}
