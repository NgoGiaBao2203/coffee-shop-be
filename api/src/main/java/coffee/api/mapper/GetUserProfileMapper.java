package coffee.api.mapper;

import coffee.api.model.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GetUserProfileMapper {
  UserProfile findByUsername(@Param("username") String username);
}
