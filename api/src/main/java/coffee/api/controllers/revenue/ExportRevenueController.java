package coffee.api.controllers.revenue;

import coffee.api.constants.Authority;
import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.result.RevenueExportResult;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.revenue.IExportRevenueService;
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
public class ExportRevenueController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final IExportRevenueService exportRevenueService;

  @PostMapping("revenues/export")
  @PreAuthorize(Authority.OWNER_OR_MANAGER)
  public ResponseEntity<byte[]> exportRevenues(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid SearchRevenueRequest request) {
    RevenueExportResult result =
        exportRevenueService.process(
            request,
            customUserDetail.getRoleName(),
            customUserDetail.getShopId(),
            customUserDetail.getUserId());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(result.getFileName()).build().toString())
        .contentType(XLSX)
        .body(result.getContent());
  }
}
