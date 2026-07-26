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

  Boolean checkDrinkExisted(@Param("drinkId") UUID drinkId);

  void deleteDrink(@Param("drinkId") UUID drinkId); //
}
