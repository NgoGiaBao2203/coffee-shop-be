package coffee.api.services.services_interface.staff;

import coffee.api.dto.request.user.DeleteStaffRequest;

public interface IDeleteStaffService {
  void process(DeleteStaffRequest request);
}
