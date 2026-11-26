package coffee.api.services.services_implement.profile;

import coffee.api.constants.ErrorMessage;
import coffee.api.dto.request.profile.ResetPasswordRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.ResetPasswordMapper;
import coffee.api.services.services_interface.profile.IResetPasswordService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements IResetPasswordService {
  private final CommonMapper commonMapper;
  private final ResetPasswordMapper resetPasswordMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      ResetPasswordRequest request, String currentUserRoleName, UUID currentUserId) {
    // 1. Only OWNER and MANAGER may reset a password; STAFF never can
    validateCallerRole(currentUserRoleName);

    // 2. Target must be in the caller's scope: OWNER -> every MANAGER and STAFF,
    //    MANAGER -> STAFF of the shop they manage; never an OWNER or the caller themselves
    UUID profileId = request.getProfileId();
    if (!Boolean.TRUE.equals(
        commonMapper.checkStaffManageable(profileId, currentUserRoleName, currentUserId))) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, profileId);
    }

    // 3. Put back the default password and verify the profile still exists
    if (resetPasswordMapper.resetPassword(profileId) == 0) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, profileId);
    }
  }

  private static void validateCallerRole(String currentUserRoleName) {
    if (!Roles.OWNER.getValue().equals(currentUserRoleName)
        && !Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      throw new AuthorizationDeniedException("Access denied");
    }
  }
}
