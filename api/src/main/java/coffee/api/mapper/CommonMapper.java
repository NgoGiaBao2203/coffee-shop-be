package coffee.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface CommonMapper {
  Boolean checkStaffExisted(@Param("profileId") UUID profileId);

  Boolean checkStaffExistedByUsername(@Param("username") String username);

  Boolean checkStaffPhoneExistedForCreate(@Param("phoneNumber") String phoneNumber);

  Boolean checkStaffEmailExisted(@Param("email") String email);

  Boolean checkStaffPhoneExisted(
    @Param("profileId") UUID profileId,
    @Param("phoneNumber") String phoneNumber
  );

  Boolean checkShopExisted(@Param("shopId") UUID shopId);

  Boolean checkCategoryExisted(
    @Param("categoryId") UUID categoryId,
    @Param("currentUserRoleName") String currentUserRoleName,
    @Param("currentUserShopId") UUID currentUserShopId
  );

  Boolean checkDrinkExisted(
    @Param("drinkId") UUID drinkId,
    @Param("currentUserRoleName") String currentUserRoleName,
    @Param("currentUserShopId") UUID currentUserShopId
  );
}
