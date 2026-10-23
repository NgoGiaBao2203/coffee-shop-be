package coffee.api.controllers.drink;

import coffee.api.dto.request.drink.DeleteDrinksRequest;
import coffee.api.dto.response.drink.DeleteDrinkResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.drink.IDeleteDrinkService;
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
public class DeleteDrinksController {
  private final IDeleteDrinkService deleteDrinksService;

  @DeleteMapping("drink/delete")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<DeleteDrinkResponse> deleteDrink(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid DeleteDrinksRequest request) {
    deleteDrinksService.process(
        request,
        customUserDetail.getUserId(),
        customUserDetail.getRoleName(),
        customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(DeleteDrinkResponse.of(ResponseCode.SUCCESS, "Drink deleted successfully"));
  }
}
