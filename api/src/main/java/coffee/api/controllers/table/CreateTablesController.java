package coffee.api.controllers.table;

import coffee.api.dto.request.table.CreateTablesRequest;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.table.ICreateTablesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CreateTablesController {
  private final ICreateTablesService createTablesService;

  @PostMapping("tables/create")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  @ResponseStatus(HttpStatus.CREATED)
  public void createTable(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CreateTablesRequest request) {
    createTablesService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getShopId());
  }
}
