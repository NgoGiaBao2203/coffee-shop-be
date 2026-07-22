package coffee.api.controllers.dropdown;

import coffee.api.dto.response.dropdown.ShopNameResponse;
import coffee.api.dto.result.ShopNameResult;
import coffee.api.enums.ResponseCode;
import coffee.api.services.services_interface.dropdown.IShopNameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RetrieveShopNameController {
  private final IShopNameService shopNameService;

  @GetMapping("dropdown/shop-name")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<ShopNameResponse> retrieveShopName() {
    List<ShopNameResult> response = shopNameService.process();
    return ResponseEntity.ok()
      .body(ShopNameResponse.of(
        ResponseCode.SUCCESS,
        "User shop name retrieved successfully",
        response
      ));
  }
}
