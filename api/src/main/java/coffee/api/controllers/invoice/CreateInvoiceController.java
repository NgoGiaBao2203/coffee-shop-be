package coffee.api.controllers.invoice;

import coffee.api.dto.request.invoice.CreateInvoiceRequest;
import coffee.api.dto.response.invoice.CreateInvoiceResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.invoice.ICreateInvoiceService;
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
public class CreateInvoiceController {

  private final ICreateInvoiceService createInvoiceService;

  @PostMapping("invoice/create")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public ResponseEntity<CreateInvoiceResponse> createInvoice(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CreateInvoiceRequest request) {

    createInvoiceService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getShopId(),
        customUserDetail.getUserId());

    return ResponseEntity.ok()
        .body(CreateInvoiceResponse.of(ResponseCode.SUCCESS, "Invoice created successfully"));
  }
}
