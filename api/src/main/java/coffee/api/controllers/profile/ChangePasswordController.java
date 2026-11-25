package coffee.api.controllers.profile;

import coffee.api.constants.Authority;
import coffee.api.dto.request.profile.ChangePasswordRequest;
import coffee.api.dto.response.profile.ChangePasswordResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.profile.IChangePasswordService;
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
public class ChangePasswordController {
  private final IChangePasswordService changePasswordService;

  @PutMapping("profile/change-password")
  @PreAuthorize(Authority.ANY_ROLE)
  public ResponseEntity<ChangePasswordResponse> changePassword(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid ChangePasswordRequest request) {
    changePasswordService.process(request, customUserDetail.getUserId());
    return ResponseEntity.ok()
        .body(ChangePasswordResponse.of(ResponseCode.SUCCESS, "Password changed successfully"));
  }
}
