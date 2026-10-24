package coffee.api.controllers.detail;

import coffee.api.dto.response.detail.GetDrinkDetailResponse;
import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.detail.IGetDrinkDetailService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetDrinkDetailController {

  private final IGetDrinkDetailService getDrinkDetailService;

  @GetMapping("drink/detail/{drinkId}")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public GetDrinkDetailResponse getDrinkDetail(
      @AuthenticationPrincipal CustomUserDetail customUserDetail, @PathVariable UUID drinkId) {

    DrinkResult result =
        getDrinkDetailService.process(
            drinkId, customUserDetail.getRoleName(), customUserDetail.getShopId());

    return GetDrinkDetailResponse.of(ResponseCode.SUCCESS, "Get drink detail successfully", result);
  }
}
