package coffee.api.controllers.invoice;

import coffee.api.constants.Authority;
import coffee.api.dto.request.invoice.PrintInvoiceRequest;
import coffee.api.dto.result.InvoicePrintResult;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.invoice.IPrintInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PrintInvoiceController {

  private final IPrintInvoiceService printInvoiceService;

  @PostMapping("invoices/print")
  @PreAuthorize(Authority.ANY_ROLE)
  public ResponseEntity<byte[]> printInvoice(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid PrintInvoiceRequest request) {
    InvoicePrintResult result =
        printInvoiceService.process(
            request,
            customUserDetail.getRoleName(),
            customUserDetail.getShopId(),
            customUserDetail.getUserId());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(result.getFileName()).build().toString())
        .contentType(MediaType.APPLICATION_PDF)
        .body(result.getContent());
  }
}
