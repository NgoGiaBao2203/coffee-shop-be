package coffee.api.controllers.invoice;

import coffee.api.dto.request.invoice.StatusInvoiceRequest;
import coffee.api.dto.response.invoice.StatusInvoiceResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.invoice.IStatusInvoiceService;
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
public class UpdateInvoiceStatusController {

  private final IStatusInvoiceService updateInvoiceStatusService;

  @PutMapping("invoice/status")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public ResponseEntity<StatusInvoiceResponse> updateInvoiceStatus(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid StatusInvoiceRequest request) {

    updateInvoiceStatusService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getShopId(),
        customUserDetail.getUserId());

    return ResponseEntity.ok()
        .body(
            StatusInvoiceResponse.of(ResponseCode.SUCCESS, "Invoice status updated successfully"));
  }
}
