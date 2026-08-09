package coffee.api.controllers.dropdown;

import coffee.api.dto.response.dropdown.RoleResponse;
import coffee.api.dto.result.RoleResult;
import coffee.api.enums.ResponseCode;
import coffee.api.services.services_interface.dropdown.IRoleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RetrieveRoleController {
  private final IRoleService roleService;

  @GetMapping("dropdown/role")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<RoleResponse> retrieveRole() {
    List<RoleResult> response = roleService.process();
    return ResponseEntity.ok()
        .body(RoleResponse.of(ResponseCode.SUCCESS, "User role retrieved successfully", response));
  }
}
