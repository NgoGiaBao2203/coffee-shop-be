package coffee.api.services.services_interface.staff;

import coffee.api.dto.request.user.SearchUsersRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.ProfileResult;
import java.util.UUID;

public interface IGetStaffsService {
  PageResponse<ProfileResult> process(
      SearchUsersRequest request, String currentUserRoleName, UUID currentUserId);
}
