package coffee.api.mapper;

import coffee.api.dto.result.InvoiceResult;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommonMapper {

  InvoiceResult lockInvoice(@Param("invoiceId") UUID invoiceId);

  UUID lockOccupiedInvoiceTable(@Param("tableId") UUID tableId, @Param("shopId") UUID shopId);

  boolean hasUnpaidTableInvoices(@Param("tableId") UUID tableId);

  int releaseInvoiceTable(@Param("tableId") UUID tableId, @Param("shopId") UUID shopId);

  UUID findDrinkIdByName(@Param("drinkName") String drinkName, @Param("shopId") UUID shopId);

  boolean checkDrinkDetailSizeExisted(
      @Param("drinkId") UUID drinkId,
      @Param("size") String size,
      @Param("excludedDrinkDetailId") UUID excludedDrinkDetailId);

  Boolean checkStaffExistedByUsername(@Param("username") String username);

  Boolean checkStaffEmailExisted(@Param("email") String email);

  Boolean checkStaffPhoneExisted(
      @Param("phoneNumber") String phoneNumber, @Param("excludedProfileId") UUID excludedProfileId);

  int countOtherActiveStaffShops(@Param("profileId") UUID profileId, @Param("shopId") UUID shopId);

  int countPendingInvoicesByStaff(@Param("profileId") UUID profileId, @Param("shopId") UUID shopId);

  Boolean checkStaffManageable(
      @Param("profileId") UUID profileId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserId") UUID currentUserId);

  String findRoleNameById(@Param("roleId") UUID roleId);

  Boolean checkShopExisted(@Param("shopId") UUID shopId);

  Boolean checkCategoryExisted(
      @Param("categoryId") UUID categoryId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  Boolean checkCategoryExistsInShop(
      @Param("categoryId") UUID categoryId, @Param("shopId") UUID shopId);

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

  Boolean checkTableExistsInShop(@Param("tableId") UUID tableId, @Param("shopId") UUID shopId);

  Boolean checkTableNumberExisted(
      @Param("tableId") UUID tableId,
      @Param("tableNumber") Integer tableNumber,
      @Param("shopId") UUID shopId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
