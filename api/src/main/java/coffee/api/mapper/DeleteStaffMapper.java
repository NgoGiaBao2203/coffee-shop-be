package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteStaffMapper {
  int softDeleteStaffShops(
      @Param("profileId") UUID profileId,
      @Param("shopId") UUID shopId,
      @Param("deletedBy") UUID deletedBy);

  int countActiveStaffShops(@Param("profileId") UUID profileId);

  int softDeleteStaff(@Param("profileId") UUID profileId);
}
