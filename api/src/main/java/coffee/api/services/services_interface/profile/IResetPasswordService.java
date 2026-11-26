package coffee.api.services.services_interface.profile;

import coffee.api.dto.request.profile.ResetPasswordRequest;
import java.util.UUID;

public interface IResetPasswordService {
  void process(ResetPasswordRequest request, String currentUserRoleName, UUID currentUserId);
}
