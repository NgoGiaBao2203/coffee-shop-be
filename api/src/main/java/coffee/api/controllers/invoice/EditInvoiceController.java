package coffee.api.controllers.invoice;

import coffee.api.dto.request.invoice.EditInvoiceRequest;
import coffee.api.dto.response.invoice.EditInvoiceResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.invoice.IEditInvoiceService;
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
public class EditInvoiceController {

  private final IEditInvoiceService editInvoiceService;

  @PutMapping("invoice/edit")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public ResponseEntity<EditInvoiceResponse> editInvoice(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid EditInvoiceRequest request) {

    editInvoiceService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getShopId());

    return ResponseEntity.ok()
        .body(EditInvoiceResponse.of(ResponseCode.SUCCESS, "Invoice updated successfully"));
  }
}
