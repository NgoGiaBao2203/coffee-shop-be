package coffee.api.controllers.table;

import coffee.api.dto.request.table.CreateTablesRequest;
import coffee.api.dto.response.table.CreateTablesResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.table.ICreateTablesService;
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
public class CreateTablesController {

  private final ICreateTablesService createTablesService;

  @PostMapping("tables/create")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<CreateTablesResponse> createTable(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CreateTablesRequest request) {
    createTablesService.process(
        request,
        customUserDetail.getUserId(),
        customUserDetail.getRoleName(),
        customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(CreateTablesResponse.of(ResponseCode.SUCCESS, "Table created successfully"));
  }
}
