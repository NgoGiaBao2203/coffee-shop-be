package coffee.api.controllers.staff;

import coffee.api.dto.request.user.CreateStaffRequest;
import coffee.api.dto.response.staff.CreateStaffResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.staff.ICreateStaffService;
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
public class CreateStaffController {
  private final ICreateStaffService createStaffService;

  @PostMapping("staff/create")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<CreateStaffResponse> editStaff(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CreateStaffRequest request) {
    createStaffService.process(
        request, customUserDetail.getUserId(), customUserDetail.getRoleName());
    return ResponseEntity.ok()
        .body(CreateStaffResponse.of(ResponseCode.SUCCESS, "Staff created successfully"));
  }
}
