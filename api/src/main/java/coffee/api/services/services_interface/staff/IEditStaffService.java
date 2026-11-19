package coffee.api.services.services_interface.staff;

import coffee.api.dto.request.staff.EditStaffRequest;
import java.util.UUID;

public interface IEditStaffService {
  void process(
      EditStaffRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId);
}
