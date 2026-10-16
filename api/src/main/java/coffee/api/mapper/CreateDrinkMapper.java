package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
      @Param("price") Float price);

  Boolean checkDrinkExistedByName(
      @Param("shopId") UUID shopId, @Param("drinkName") String drinkName);
}
