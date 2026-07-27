package coffee.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface CreateDrinkMapper {

  void createDrink(
    @Param("drinkCategoryId") UUID drinkCategoryId,
    @Param("drinkDetailId") UUID drinkDetailId,
    @Param("shopId") UUID shopId,
    @Param("drinkName") String drinkName,
    @Param("imageUrl") String imageUrl,
    @Param("status") Integer status,
    @Param("isDeleted") Boolean isDeleted,
    @Param("size") String size,
    @Param("price") Float price
  );

  Boolean checkDrinkExistedByName(
    @Param("shopId") UUID shopId,
    @Param("drinkName") String drinkName
  );
}