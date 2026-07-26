package coffee.api.repository;

import coffee.api.model.UserProfile;
import coffee.api.mapper.CustomUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomUserRepository {
  private final CustomUserMapper customUserMapper;

  public UserProfile findByProfileId(UUID userId) {
    return customUserMapper.findByProfileId(userId);
  }

  public UserProfile findByEmail(String email) {
    return customUserMapper.findByEmail(email);
  }
}
