package coffee.api.services.services_interface.staff;

import coffee.api.dto.request.staff.DeleteStaffRequest;
import java.util.UUID;

public interface IDeleteStaffService {
  void process(
      DeleteStaffRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId);
}
