package coffee.api.controllers.shop;

import coffee.api.dto.request.shop.DeleteShopRequest;
import coffee.api.dto.response.shop.DeleteShopResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.shop.IDeleteShopService;
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
public class DeleteShopController {

  private final IDeleteShopService deleteShopBranchService;

  @DeleteMapping("shop/delete")
  @PreAuthorize("hasAnyRole('OWNER')")
  public ResponseEntity<DeleteShopResponse> deleteShopBranch(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid DeleteShopRequest request) {
    deleteShopBranchService.process(request, customUserDetail.getUserId());

    return ResponseEntity.ok()
        .body(DeleteShopResponse.of(ResponseCode.SUCCESS, "Shop deleted successfully"));
  }
}
