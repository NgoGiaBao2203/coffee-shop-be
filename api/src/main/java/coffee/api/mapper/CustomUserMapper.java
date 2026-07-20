package coffee.api.mapper;

import coffee.api.model.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface CustomUserMapper {
  UserProfile findByProfileId(@Param("userId") UUID userId);

  UserProfile findByEmail(@Param("email") String email);
}
