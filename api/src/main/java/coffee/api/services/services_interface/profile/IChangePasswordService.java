package coffee.api.services.services_interface.profile;

import coffee.api.dto.request.profile.ChangePasswordRequest;
import java.util.UUID;

public interface IChangePasswordService {
  void process(ChangePasswordRequest request, UUID currentUserId);
}
