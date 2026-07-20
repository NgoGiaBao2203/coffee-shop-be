package coffee.api.controllers.common;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.response.drink.GetDrinkResponse;
import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.ResponseCode;
import coffee.api.services.services_interface.drink.IGetDrinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetDrinksController {
  private final IGetDrinkService drinkService;

  @PostMapping("common/get-drinks")
  public ResponseEntity<GetDrinkResponse> getDrinks(
    @RequestBody @Valid SearchDrinksRequest request
  ) {
    PageResponse<DrinkResult> response = drinkService.process(request);
    return ResponseEntity.ok()
      .body(GetDrinkResponse.of(
        ResponseCode.SUCCESS,
        "Drinks retrieved successfully",
        response
      ));
  }
}