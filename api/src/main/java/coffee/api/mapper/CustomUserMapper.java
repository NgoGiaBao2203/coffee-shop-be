package coffee.api.mapper;

import coffee.api.model.UserProfile;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomUserMapper {
  UserProfile findByProfileId(@Param("userId") UUID userId);

  UserProfile findByEmail(@Param("email") String email);
}
