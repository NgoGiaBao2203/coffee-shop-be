package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateStaffMapper {
  void updateStaff(
      @Param("profileId") UUID profileId,
      @Param("fullName") String fullName,
      @Param("phoneNumber") String phoneNumber,
      @Param("roleId") UUID roleId,
      @Param("shopId") UUID shopId,
      @Param("currentUserRoleName") String currentUserRoleName);
}
