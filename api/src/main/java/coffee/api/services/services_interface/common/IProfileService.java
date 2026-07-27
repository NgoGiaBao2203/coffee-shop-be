package coffee.api.services.services_interface.common;

import coffee.api.dto.result.ProfileResult;
import coffee.api.dto.request.common.UserProfileRequest;

public interface IProfileService {
  ProfileResult process(UserProfileRequest request);
}
