package coffee.api.controllers.table;

import coffee.api.constants.Authority;
import coffee.api.dto.request.table.ReserveTablesRequest;
import coffee.api.dto.response.table.ReserveTablesResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.table.IReserveTablesService;
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
public class ReserveTablesController {

  private final IReserveTablesService reserveTablesService;

  @PutMapping("table/reserve")
  @PreAuthorize(Authority.ANY_ROLE)
  public ResponseEntity<ReserveTablesResponse> reserveTable(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid ReserveTablesRequest request) {

    reserveTablesService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getShopId(),
        customUserDetail.getUserId());

    return ResponseEntity.ok()
        .body(ReserveTablesResponse.of(ResponseCode.SUCCESS, "Table reserved successfully"));
  }
}
