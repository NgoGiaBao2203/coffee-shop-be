package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteCategoryMapper {

  int countPendingInvoicesByCategory(@Param("categoryId") UUID categoryId);

  void softDeleteDrinkDetailsByCategory(
      @Param("categoryId") UUID categoryId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId,
      @Param("deletedBy") UUID deletedBy);

  void softDeleteDrinksByCategory(
      @Param("categoryId") UUID categoryId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId,
      @Param("deletedBy") UUID deletedBy);

  int softDeleteCategory(
      @Param("categoryId") UUID categoryId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId,
      @Param("deletedBy") UUID deletedBy);
}
