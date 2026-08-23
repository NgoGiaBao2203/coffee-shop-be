package coffee.api.controllers.shop_branch;

import coffee.api.dto.request.shop_branch.CreateShopBranchRequest;
import coffee.api.dto.response.shop_branch.CreateShopBranchResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.shop_branch.ICreateShopBranchService;
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
public class CreateShopBranchController {

  private final ICreateShopBranchService createShopBranchService;

  @PostMapping("shop-branch/create")
  @PreAuthorize("hasAnyRole('OWNER')")
  public ResponseEntity<CreateShopBranchResponse> createShopBranch(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CreateShopBranchRequest request) {
    createShopBranchService.process(request);
    return ResponseEntity.ok()
        .body(
            CreateShopBranchResponse.of(ResponseCode.SUCCESS, "shop branch created successfully"));
  }
}
