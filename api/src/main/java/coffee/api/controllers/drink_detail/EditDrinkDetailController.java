package coffee.api.controllers.drink_detail;

import coffee.api.dto.request.drink_detail.EditDrinkDetailRequest;
import coffee.api.dto.response.drink_detail.EditDrinkDetailResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.drink_detail.IEditDrinkDetailService;
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
public class EditDrinkDetailController {

  private final IEditDrinkDetailService editDrinkDetailService;

  @PutMapping("drink-detail/edit")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<EditDrinkDetailResponse> editDrinkDetail(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid EditDrinkDetailRequest request) {
    editDrinkDetailService.process(
        request,
        customUserDetail.getUserId(),
        customUserDetail.getRoleName(),
        customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(
            EditDrinkDetailResponse.of(ResponseCode.SUCCESS, "Drink detail updated successfully"));
  }
}
