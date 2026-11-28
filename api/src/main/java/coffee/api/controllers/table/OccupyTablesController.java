package coffee.api.controllers.table;

import coffee.api.constants.Authority;
import coffee.api.dto.request.table.OccupyTablesRequest;
import coffee.api.dto.response.table.OccupyTablesResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.table.IOccupyTablesService;
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
public class OccupyTablesController {

  private final IOccupyTablesService occupyTablesService;

  @PutMapping("table/occupy")
  @PreAuthorize(Authority.ANY_ROLE)
  public ResponseEntity<OccupyTablesResponse> occupyTable(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid OccupyTablesRequest request) {

    occupyTablesService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getShopId(),
        customUserDetail.getUserId());

    return ResponseEntity.ok()
        .body(OccupyTablesResponse.of(ResponseCode.SUCCESS, "Table occupied successfully"));
  }
}
