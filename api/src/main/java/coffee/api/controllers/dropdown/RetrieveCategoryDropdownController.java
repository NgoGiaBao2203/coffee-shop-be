package coffee.api.controllers.dropdown;

import coffee.api.dto.response.dropdown.CategoryDropdownResponse;
import coffee.api.dto.result.CategoryDropdownResult;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.dropdown.ICategoryDropdownService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RetrieveCategoryDropdownController {
  private final ICategoryDropdownService categoryDropdownService;

  @GetMapping("dropdown/category")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<CategoryDropdownResponse> retrieveCategory(
      @AuthenticationPrincipal CustomUserDetail customUserDetail) {
    List<CategoryDropdownResult> response =
        categoryDropdownService.process(
            customUserDetail.getRoleName(), customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(
            CategoryDropdownResponse.of(
                ResponseCode.SUCCESS, "Category dropdown retrieved successfully", response));
  }
}
