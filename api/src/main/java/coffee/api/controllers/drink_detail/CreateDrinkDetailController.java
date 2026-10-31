package coffee.api.controllers.drink_detail;

import coffee.api.dto.request.drink_detail.CreateDrinkDetailRequest;
import coffee.api.dto.response.drink_detail.CreateDrinkDetailResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.drink_detail.ICreateDrinkDetailService;
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
public class CreateDrinkDetailController {

  private final ICreateDrinkDetailService createDrinkDetailService;

  @PostMapping("drink-detail/create")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<CreateDrinkDetailResponse> createDrinkDetail(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CreateDrinkDetailRequest request) {
    createDrinkDetailService.process(
        request,
        customUserDetail.getUserId(),
        customUserDetail.getRoleName(),
        customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(
            CreateDrinkDetailResponse.of(
                ResponseCode.SUCCESS, "Drink detail created successfully"));
  }
}
