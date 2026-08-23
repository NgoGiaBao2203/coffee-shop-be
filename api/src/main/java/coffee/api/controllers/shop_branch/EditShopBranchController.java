package coffee.api.controllers.shop_branch;

import coffee.api.dto.request.shop_branch.EditShopBranchRequest;
import coffee.api.dto.response.shop_branch.EditShopBranchResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.shop_branch.IEditShopBranchService;
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
public class EditShopBranchController {

  private final IEditShopBranchService editShopBranchService;

  @PostMapping("shop-branch/edit")
  @PreAuthorize("hasAnyRole('OWNER')")
  public ResponseEntity<EditShopBranchResponse> editShopBranch(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid EditShopBranchRequest request) {

    editShopBranchService.process(request);

    return ResponseEntity.ok()
        .body(EditShopBranchResponse.of(ResponseCode.SUCCESS, "Shop branch updated successfully"));
  }
}
