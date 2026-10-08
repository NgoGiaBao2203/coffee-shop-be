package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateDrinkMapper {

  boolean checkDrinkNameExistedForEdit(
      @Param("drinkId") UUID drinkId, @Param("drinkName") String drinkName);

  void updateDrink(
      @Param("drinkId") UUID drinkId,
      @Param("drinkName") String drinkName,
      @Param("imageUrl") String imageUrl,
      @Param("status") Integer status,
      @Param("drinkCategoryId") UUID drinkCategoryId,
      @Param("shopId") UUID shopId,
      @Param("currentUserRoleName") String currentUserRoleName);

  String getDrinkImageUrl(
      @Param("drinkId") UUID drinkId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
