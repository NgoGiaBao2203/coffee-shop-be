package coffee.api.services.services_implement.profile;

import coffee.api.constants.ErrorMessage;
import coffee.api.dto.request.profile.ChangePasswordRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdatePasswordMapper;
import coffee.api.services.services_interface.profile.IChangePasswordService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordServiceImpl implements IChangePasswordService {
  private final UpdatePasswordMapper updatePasswordMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(ChangePasswordRequest request, UUID currentUserId) {

    // 1. Passwords are always trimmed
    String currentPassword = request.getCurrentPassword().trim();
    String newPassword = request.getNewPassword().trim();
    String confirmPassword = request.getConfirmPassword().trim();

    // 2. Whitespace left inside a trimmed new password is never accepted
    if (containsWhitespace(newPassword) || containsWhitespace(confirmPassword)) {
      throw new InvalidRequestException(ErrorMessage.PASSWORD_CONTAINS_WHITESPACE);
    }

    // 3. Confirmation must repeat the new password exactly
    if (!newPassword.equals(confirmPassword)) {
      throw new InvalidRequestException(ErrorMessage.CONFIRM_PASSWORD_MISMATCH);
    }

    // 4. Load the current hash of the caller's active profile
    String currentHash = updatePasswordMapper.findActivePasswordByProfileId(currentUserId);
    if (currentHash == null) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, currentUserId);
    }

    // 5. The caller must prove they know the current password
    if (!passwordEncoder.matches(currentPassword, currentHash)) {
      throw new InvalidRequestException(ErrorMessage.CURRENT_PASSWORD_INCORRECT);
    }

    // 6. The new password must differ from the current one
    if (passwordEncoder.matches(newPassword, currentHash)) {
      throw new InvalidRequestException(ErrorMessage.NEW_PASSWORD_SAME_AS_CURRENT);
    }

    // 7. Store the new hash and verify the profile still exists
    int affectedRows =
        updatePasswordMapper.updatePassword(currentUserId, passwordEncoder.encode(newPassword));
    if (affectedRows == 0) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, currentUserId);
    }
  }

  // Covers spaces as well as tabs and line breaks
  private static boolean containsWhitespace(String value) {
    return value.chars().anyMatch(Character::isWhitespace);
  }
}
