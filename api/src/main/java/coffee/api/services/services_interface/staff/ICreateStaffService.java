package coffee.api.services.services_interface.staff;

import coffee.api.dto.request.user.CreateStaffRequest;
import java.util.UUID;

public interface ICreateStaffService {
  void process(CreateStaffRequest request, UUID currentUserId, String currentUserRoleName);
}
