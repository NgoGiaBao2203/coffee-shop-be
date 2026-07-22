package coffee.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface CreateStaffMapper {
  void createStaff(
    @Param("email") String email,
    @Param("username") String username,
    @Param("fullName") String fullName,
    @Param("phoneNumber") String phoneNumber,
    @Param("roleId") UUID roleId,
    @Param("shopId") UUID shopId,
    @Param("currentUserId") UUID currentUserId,
    @Param("currentUserRoleName") String currentUserRoleName
  );
}
