package coffee.api.controllers.shop;

import coffee.api.dto.request.shop.CreateShopRequest;
import coffee.api.dto.response.shop.CreateShopResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.shop.ICreateShopService;
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
public class CreateShopController {

  private final ICreateShopService createShopService;

  @PostMapping("shop/create")
  @PreAuthorize("hasAnyRole('OWNER')")
  public ResponseEntity<CreateShopResponse> createShop(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CreateShopRequest request) {

    createShopService.process(
        request, customUserDetail.getUserId(), customUserDetail.getRoleName());

    return ResponseEntity.ok()
        .body(CreateShopResponse.of(ResponseCode.SUCCESS, "Shop created successfully"));
  }
}
