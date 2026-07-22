package coffee.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface DeleteStaffMapper {
  void deleteStaff(@Param("profileId")UUID profileId);
}
