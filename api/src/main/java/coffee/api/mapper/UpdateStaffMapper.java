package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateStaffMapper {
  int updateStaff(
      @Param("profileId") UUID profileId,
      @Param("fullName") String fullName,
      @Param("phoneNumber") String phoneNumber,
      @Param("roleId") UUID roleId,
      @Param("currentUserRoleName") String currentUserRoleName);

  int updateStaffShop(
      @Param("profileId") UUID profileId,
      @Param("shopId") UUID shopId,
      @Param("roleId") UUID roleId);

  int removeOtherStaffShops(
      @Param("profileId") UUID profileId,
      @Param("shopId") UUID shopId,
      @Param("deletedBy") UUID deletedBy);

  void insertStaffShop(
      @Param("profileId") UUID profileId,
      @Param("shopId") UUID shopId,
      @Param("roleId") UUID roleId);
}
