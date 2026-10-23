package coffee.api.mapper;

import java.math.BigDecimal;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreateDrinkMapper {

  void insertDrink(
      @Param("drinkId") UUID drinkId,
      @Param("drinkCategoryId") UUID drinkCategoryId,
      @Param("shopId") UUID shopId,
      @Param("drinkName") String drinkName,
      @Param("imageUrl") String imageUrl,
      @Param("status") Integer status);

  void insertDrinkDetail(
      @Param("drinkId") UUID drinkId, @Param("size") String size, @Param("price") BigDecimal price);
}
