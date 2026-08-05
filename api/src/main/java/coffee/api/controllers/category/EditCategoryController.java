package coffee.api.controllers.category;

import coffee.api.dto.request.category.EditCategoriesRequest;
import coffee.api.dto.response.category.EditCategoriesResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.category.IEditCategoriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EditCategoryController {
  private final IEditCategoriesService editCategoriesService;

  @PutMapping("category/edit")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<EditCategoriesResponse> editCategories(
    @AuthenticationPrincipal CustomUserDetail customUserDetail,
    @RequestBody @Valid EditCategoriesRequest request
  ) {
    editCategoriesService.process(
      request,
      customUserDetail.getRoleName(),
      customUserDetail.getUserId(),
      customUserDetail.getShopId()
    );
    return ResponseEntity.ok().body(
      EditCategoriesResponse.of(
        ResponseCode.SUCCESS,
        "Category edited successfully"
      )
    );
  }
}
