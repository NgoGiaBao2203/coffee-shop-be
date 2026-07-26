package coffee.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface CreateDrinkMapper {

  void createDrink(
    @Param("drinkId") UUID drinkId,
    @Param("drinkName") String drinkName,
    @Param("imageUrl") String imageUrl,
    @Param("status") Integer status,
    @Param("isDeleted") Boolean isDeleted,
    @Param("shopId") UUID shopId,
    @Param("drinkCategoryId") UUID drinkCategoryId
  );

  void createDrinkDetail(
    @Param("drinkDetailId") UUID drinkDetailId,
    @Param("size") String size,
    @Param("price") Float price,
    @Param("drinkId") UUID drinkId
  );

  Boolean checkDrinkExistedByName(
    @Param("drinkName") String drinkName,
    @Param("shopId") UUID shopId
  );
}