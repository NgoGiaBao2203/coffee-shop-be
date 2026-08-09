package coffee.api.controllers.staff;

import coffee.api.dto.request.user.EditStaffRequest;
import coffee.api.dto.response.staff.EditStaffResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.staff.IEditStaffService;
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
public class EditStaffController {
  private final IEditStaffService editStaffService;

  @PutMapping("staff/edit")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<EditStaffResponse> editStaff(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid EditStaffRequest request) {
    editStaffService.process(request, customUserDetail.getRoleName());
    return ResponseEntity.ok()
        .body(EditStaffResponse.of(ResponseCode.SUCCESS, "Staff edited successfully"));
  }
}
