package coffee.api.controllers.shop_branch;

import coffee.api.dto.request.shop_branch.SearchShopBranchRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.ShopBranchResult;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.shop_branch.IGetShopBranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetShopBranchController {

  private final IGetShopBranchService getShopBranchService;

  @PostMapping("shop-branches")
  @PreAuthorize("hasAnyRole('OWNER')")
  public PageResponse<ShopBranchResult> shopBranchResult(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid SearchShopBranchRequest request) {

    return getShopBranchService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getShopId());
  }
}
