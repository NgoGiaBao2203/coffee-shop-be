package coffee.api.controllers.table;

import coffee.api.constants.Authority;
import coffee.api.dto.request.table.ReleaseTablesRequest;
import coffee.api.dto.response.table.ReleaseTablesResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.table.IReleaseTablesService;
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
public class ReleaseTablesController {

  private final IReleaseTablesService releaseTablesService;

  @PutMapping("table/release")
  @PreAuthorize(Authority.ANY_ROLE)
  public ResponseEntity<ReleaseTablesResponse> releaseTable(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid ReleaseTablesRequest request) {

    releaseTablesService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getShopId(),
        customUserDetail.getUserId());

    return ResponseEntity.ok()
        .body(ReleaseTablesResponse.of(ResponseCode.SUCCESS, "Table released successfully"));
  }
}
