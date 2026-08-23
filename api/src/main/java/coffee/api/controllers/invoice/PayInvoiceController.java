package coffee.api.controllers.invoice;

import coffee.api.dto.request.invoice.PayInvoiceRequest;
import coffee.api.dto.response.invoice.PayInvoiceResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.invoice.IPayInvoiceService;
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
public class PayInvoiceController {

  private final IPayInvoiceService payInvoiceService;

  @PutMapping("invoice/pay")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public ResponseEntity<PayInvoiceResponse> payInvoice(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid PayInvoiceRequest request) {

    payInvoiceService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getShopId());

    return ResponseEntity.ok()
        .body(PayInvoiceResponse.of(ResponseCode.SUCCESS, "Invoice paid successfully"));
  }
}
