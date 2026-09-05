package coffee.api.controllers.table;

import coffee.api.dto.request.table.DeleteTablesRequest;
import coffee.api.dto.response.table.DeleteTablesResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.table.IDeleteTablesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeleteTablesController {
  private final IDeleteTablesService deleteTablesService;

  @DeleteMapping("table/delete")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<DeleteTablesResponse> deleteTable(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid DeleteTablesRequest request) {
    deleteTablesService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getUserId(),
        customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(DeleteTablesResponse.of(ResponseCode.SUCCESS, "Table deleted successfully"));
  }
}
