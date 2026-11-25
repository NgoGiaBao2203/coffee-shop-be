package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdatePasswordMapper {
  String findActivePasswordByProfileId(@Param("profileId") UUID profileId);

  int updatePassword(@Param("profileId") UUID profileId, @Param("password") String password);
}
