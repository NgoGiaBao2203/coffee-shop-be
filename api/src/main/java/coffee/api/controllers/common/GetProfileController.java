package coffee.api.controllers.common;

import coffee.api.dto.request.common.UserProfileRequest;
import coffee.api.dto.response.common.UserProfileResponse;
import coffee.api.dto.result.ProfileResult;
import coffee.api.enums.ResponseCode;
import coffee.api.services.services_interface.common.IProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetProfileController {
  private final IProfileService profileService;

  @PostMapping("common/get-profile")
  public ResponseEntity<UserProfileResponse> getProfile(
      @RequestBody @Valid UserProfileRequest request) {
    ProfileResult response = profileService.process(request);
    return ResponseEntity.ok()
        .body(
            UserProfileResponse.of(
                ResponseCode.SUCCESS, "User profile retrieved successfully", response));
  }
}
