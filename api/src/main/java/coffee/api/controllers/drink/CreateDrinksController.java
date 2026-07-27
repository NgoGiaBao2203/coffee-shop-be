package coffee.api.controllers.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.dto.response.drink.CreateDrinkResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.drink.ICreateDrinkService;
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
public class CreateDrinksController {

  private final ICreateDrinkService createDrinkService;

  @PostMapping("drink/create")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<CreateDrinkResponse> createDrink(
    @AuthenticationPrincipal CustomUserDetail customUserDetail,
    @RequestBody @Valid CreateDrinksRequest request
  ) {
    createDrinkService.process(
      request,
      customUserDetail.getShopId(),
      customUserDetail.getRoleName()
    );

    return ResponseEntity.ok()
      .body(CreateDrinkResponse.of(
        ResponseCode.SUCCESS,
        "Drink created successfully"
      ));
  }
}