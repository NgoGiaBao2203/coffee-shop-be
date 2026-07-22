package coffee.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface UpdateStaffMapper {
  void updateStaff(
    @Param("profileId") UUID profileId,
    @Param("fullName") String fullName,
    @Param("phoneNumber") String phoneNumber,
    @Param("roleId") UUID roleId,
    @Param("shopId") UUID shopId,
    @Param("currentUserRoleName") String currentUserRoleName
  );
}
