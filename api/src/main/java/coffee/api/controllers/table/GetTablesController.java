package coffee.api.controllers.table;

import coffee.api.dto.request.table.SearchTablesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.TableResult;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.table.IGetTablesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetTablesController {
  private final IGetTablesService getTablesService;

  @PostMapping("tables")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
  public PageResponse<TableResult> getTablesList(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody(required = false) @Valid SearchTablesRequest request) {
    if (request == null) {
      request = new SearchTablesRequest();
    }
    return getTablesService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getUserId());
  }
}
