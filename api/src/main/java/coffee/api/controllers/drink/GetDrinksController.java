package coffee.api.controllers.drink;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.DrinkResult;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.drink.IGetDrinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetDrinksController {

  private final IGetDrinkService getDrinksService;

  @PostMapping("drinks")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public PageResponse<DrinkResult> getDrinks(
    @AuthenticationPrincipal CustomUserDetail customUserDetail,
    @RequestBody @Valid SearchDrinksRequest request
  ) {

    return getDrinksService.process(
      request,
      customUserDetail.getRoleName(),
      customUserDetail.getShopId()
    );
  }
}