package coffee.api.controllers.shop;

import coffee.api.dto.request.shop.EditShopRequest;
import coffee.api.dto.response.shop.EditShopResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.services.services_interface.shop.IEditShopService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EditShopController {

  private final IEditShopService editShopBranchService;

  @PutMapping("shop/{shopId}")
  @PreAuthorize("hasAnyRole('OWNER')")
  public ResponseEntity<EditShopResponse> editShopBranch(
      @PathVariable UUID shopId, @RequestBody @Valid EditShopRequest request) {
    editShopBranchService.process(shopId, request);

    return ResponseEntity.ok()
        .body(EditShopResponse.of(ResponseCode.SUCCESS, "Shop updated successfully"));
  }
}
