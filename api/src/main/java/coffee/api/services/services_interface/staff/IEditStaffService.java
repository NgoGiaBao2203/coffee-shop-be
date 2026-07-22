package coffee.api.services.services_interface.staff;

import coffee.api.dto.request.user.EditStaffRequest;

public interface IEditStaffService {
  void process(EditStaffRequest request, String currentUserRoleName);
}
