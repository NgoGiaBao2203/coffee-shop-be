package coffee.api.controllers.staff;

import coffee.api.constants.Authority;
import coffee.api.dto.request.staff.DeleteStaffRequest;
import coffee.api.dto.response.staff.DeleteStaffResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.staff.IDeleteStaffService;
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
public class DeleteStaffController {
  private final IDeleteStaffService deleteStaffService;

  @DeleteMapping("staff/delete")
  @PreAuthorize(Authority.OWNER_OR_MANAGER)
  public ResponseEntity<DeleteStaffResponse> deleteStaff(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid DeleteStaffRequest request) {
    deleteStaffService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getUserId(),
        customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(DeleteStaffResponse.of(ResponseCode.SUCCESS, "Staff deleted successfully"));
  }
}
