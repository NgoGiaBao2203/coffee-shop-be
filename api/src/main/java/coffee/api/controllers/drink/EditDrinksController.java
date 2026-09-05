package coffee.api.controllers.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.dto.response.drink.EditDrinkResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.drink.IEditDrinksService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class EditDrinksController {
  private final IEditDrinksService editDrinksService;

  @PutMapping(value = "drink/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<EditDrinkResponse> editDrink(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestPart("data") @Valid EditDrinksRequest request,
      @RequestPart(value = "image", required = false) MultipartFile imageFile) {
    editDrinksService.process(
        request, imageFile, customUserDetail.getRoleName(), customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(EditDrinkResponse.of(ResponseCode.SUCCESS, "Drink updated successfully"));
  }
}
