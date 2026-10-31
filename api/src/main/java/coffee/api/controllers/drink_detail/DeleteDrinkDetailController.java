package coffee.api.controllers.drink_detail;

import coffee.api.dto.request.drink_detail.DeleteDrinkDetailRequest;
import coffee.api.dto.response.drink_detail.DeleteDrinkDetailResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.drink_detail.IDeleteDrinkDetailService;
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
public class DeleteDrinkDetailController {

  private final IDeleteDrinkDetailService deleteDrinkDetailService;

  @DeleteMapping("drink-detail/delete")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<DeleteDrinkDetailResponse> deleteDrinkDetail(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid DeleteDrinkDetailRequest request) {
    deleteDrinkDetailService.process(
        request,
        customUserDetail.getUserId(),
        customUserDetail.getRoleName(),
        customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(
            DeleteDrinkDetailResponse.of(
                ResponseCode.SUCCESS, "Drink detail deleted successfully"));
  }
}
