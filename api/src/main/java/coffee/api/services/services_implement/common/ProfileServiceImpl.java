package coffee.api.services.services_implement.common;

import coffee.api.dto.request.common.UserProfileRequest;
import coffee.api.dto.result.ProfileResult;
import coffee.api.exceptions.AccountDisableException;
import coffee.api.exceptions.InvalidUsernameOrPasswordException;
import coffee.api.mapper.GetUserProfileMapper;
import coffee.api.model.UserProfile;
import coffee.api.services.services_interface.common.IProfileService;
import coffee.api.utils.ConvertRoleVN;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements IProfileService {
  private final GetUserProfileMapper getUserProfileMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public ProfileResult process(UserProfileRequest request) {
    UserProfile profile = retrieveUserProfile(request.getUsername());
    if (!passwordEncoder.matches(request.getPassword(), profile.getPassword())) {
      throw new InvalidUsernameOrPasswordException("Invalid password");
    }
    return constructProfileResult(profile);
  }

  private UserProfile retrieveUserProfile(String email) {
    UserProfile profile = getUserProfileMapper.findByUsername(email);
    if (profile == null) {
      throw new InvalidUsernameOrPasswordException("Username not found");
    }
    if (profile.getIsDeleted() == null || profile.getIsDeleted()) {
      throw new AccountDisableException("Profile was disabled", profile.getEmail());
    }
    return profile;
  }

  private ProfileResult constructProfileResult(UserProfile profile) {
    ProfileResult result = new ProfileResult();
    result.setProfileId(profile.getProfileId());
    result.setFullName(profile.getFullName());
    result.setEmail(profile.getEmail());
    result.setUsername(profile.getUsername());
    result.setPhoneNumber(profile.getPhoneNumber());
    String vnRole = ConvertRoleVN.toVietnamese(profile.getRoleName());
    result.setRoleName(vnRole);
    result.setCreatedAt(profile.getCreatedAt());
    result.setUpdatedAt(profile.getUpdatedAt());
    result.setIsDeleted(profile.getIsDeleted());
    return result;
  }
}
