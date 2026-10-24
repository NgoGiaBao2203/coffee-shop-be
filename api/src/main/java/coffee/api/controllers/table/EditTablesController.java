package coffee.api.controllers.table;

import coffee.api.dto.request.table.EditTablesRequest;
import coffee.api.dto.response.table.EditTablesResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.table.IEditTablesService;
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
public class EditTablesController {
  private final IEditTablesService editTablesService;

  @PutMapping("table/update")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<EditTablesResponse> editTable(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid EditTablesRequest request) {
    editTablesService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getUserId(),
        customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(EditTablesResponse.of(ResponseCode.SUCCESS, "Table updated successfully"));
  }
}
