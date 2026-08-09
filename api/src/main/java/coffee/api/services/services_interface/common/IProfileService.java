package coffee.api.services.services_interface.common;

import coffee.api.dto.request.common.UserProfileRequest;
import coffee.api.dto.result.ProfileResult;

public interface IProfileService {
  ProfileResult process(UserProfileRequest request);
}
