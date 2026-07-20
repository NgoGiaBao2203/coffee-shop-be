package coffee.api.repository.common;

import coffee.api.model.UserProfile;
import coffee.api.mapper.GetUserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GetUserProfileRepository {
  private final GetUserProfileMapper getUserProfileMapper;

  public UserProfile findByUsername(String username) {
    return getUserProfileMapper.findByUsername(username);
  }
}
