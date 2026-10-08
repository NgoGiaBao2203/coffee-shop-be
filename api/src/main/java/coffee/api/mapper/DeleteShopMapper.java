package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteShopMapper {

  Boolean checkShopExistedById(@Param("shopId") UUID shopId);

  int countActiveInvoicesByShopId(@Param("shopId") UUID shopId);

  int countActiveStaffByShopId(@Param("shopId") UUID shopId);

  void softDeleteDrinkDetailsByShopId(
      @Param("shopId") UUID shopId, @Param("deletedBy") UUID deletedBy);

  void softDeleteDrinksByShopId(@Param("shopId") UUID shopId, @Param("deletedBy") UUID deletedBy);

  void softDeleteCategoriesByShopId(
      @Param("shopId") UUID shopId, @Param("deletedBy") UUID deletedBy);

  void softDeleteTablesByShopId(@Param("shopId") UUID shopId, @Param("deletedBy") UUID deletedBy);

  void softDeleteProfileShopsByShopId(
      @Param("shopId") UUID shopId, @Param("deletedBy") UUID deletedBy);

  void softDeleteShop(@Param("shopId") UUID shopId, @Param("deletedBy") UUID deletedBy);
}
