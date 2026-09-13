package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommonMapper {

  Boolean checkStaffExisted(@Param("profileId") UUID profileId);

  Boolean checkStaffExistedByUsername(@Param("username") String username);

  Boolean checkStaffPhoneExistedForCreate(@Param("phoneNumber") String phoneNumber);

  Boolean checkStaffEmailExisted(@Param("email") String email);

  Boolean checkStaffPhoneExisted(
      @Param("profileId") UUID profileId, @Param("phoneNumber") String phoneNumber);

  Boolean checkShopExisted(@Param("shopId") UUID shopId);

  Boolean checkCategoryExisted(
      @Param("categoryId") UUID categoryId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  Boolean checkDrinkExisted(
      @Param("drinkId") UUID drinkId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  Boolean checkShopIdIsExisted(
      @Param("currentUserId") UUID currentUserId,
      @Param("currentUserShopId") UUID currentUserShopId);

  Boolean checkCategoryNameExisted(
      @Param("categoryId") UUID categoryId,
      @Param("categoryName") String categoryName,
      @Param("shopId") UUID shopId);

  Boolean checkTableExisted(
      @Param("tableId") UUID tableId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  Boolean checkTableNumberExisted(
      @Param("tableId") UUID tableId,
      @Param("tableNumber") Integer tableNumber,
      @Param("shopId") UUID shopId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
