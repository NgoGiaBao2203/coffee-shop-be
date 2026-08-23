package coffee.api.controllers.invoice;

import coffee.api.dto.request.invoice.SearchInvoiceRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.invoice.IGetInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetInvoiceController {

  private final IGetInvoiceService getInvoiceService;

  @PostMapping("invoices")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public PageResponse<InvoiceResult> getInvoices(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid SearchInvoiceRequest request) {

    return getInvoiceService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getShopId());
  }
}
