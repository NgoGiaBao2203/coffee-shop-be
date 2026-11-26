package coffee.api.controllers.profile;

import coffee.api.constants.Authority;
import coffee.api.dto.request.profile.ResetPasswordRequest;
import coffee.api.dto.response.profile.ResetPasswordResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.profile.IResetPasswordService;
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
public class ResetPasswordController {
  private final IResetPasswordService resetPasswordService;

  @PutMapping("profile/reset-password")
  @PreAuthorize(Authority.OWNER_OR_MANAGER)
  public ResponseEntity<ResetPasswordResponse> resetPassword(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid ResetPasswordRequest request) {
    resetPasswordService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getUserId());
    return ResponseEntity.ok()
        .body(
            ResetPasswordResponse.of(
                ResponseCode.SUCCESS, "Password reset to default successfully"));
  }
}
