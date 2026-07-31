package coffee.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface UpdateDrinkMapper {

  void updateDrink(
    @Param("drinkId") UUID drinkId,
    @Param("drinkName") String drinkName,
    @Param("imageUrl") String imageUrl,
    @Param("status") Integer status,
    @Param("drinkCategoryId") UUID drinkCategoryId,
    @Param("price") Float price,
    @Param("size") String size,
    @Param("shopId") UUID shopId,
    @Param("currentUserRoleName") String currentUserRoleName
  );
}