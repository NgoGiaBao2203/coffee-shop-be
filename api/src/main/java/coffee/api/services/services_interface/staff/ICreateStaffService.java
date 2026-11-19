package coffee.api.services.services_interface.staff;

import coffee.api.dto.request.staff.CreateStaffRequest;
import java.util.UUID;

public interface ICreateStaffService {
  void process(
      CreateStaffRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId);
}
