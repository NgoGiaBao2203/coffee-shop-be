package coffee.api.controllers.invoice;

import coffee.api.dto.request.invoice.CancelInvoiceRequest;
import coffee.api.dto.response.invoice.CancelInvoiceResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.invoice.ICancelInvoiceService;
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
public class CancelInvoiceController {

  private final ICancelInvoiceService cancelInvoiceService;

  @PutMapping("invoice/cancel")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public ResponseEntity<CancelInvoiceResponse> cancelInvoice(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CancelInvoiceRequest request) {

    cancelInvoiceService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getShopId(),
        customUserDetail.getUserId());

    return ResponseEntity.ok()
        .body(CancelInvoiceResponse.of(ResponseCode.SUCCESS, "Invoice cancelled successfully"));
  }
}
