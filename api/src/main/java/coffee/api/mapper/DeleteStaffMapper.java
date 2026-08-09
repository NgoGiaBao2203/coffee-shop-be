package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteStaffMapper {
  void deleteStaff(@Param("profileId") UUID profileId);
}
