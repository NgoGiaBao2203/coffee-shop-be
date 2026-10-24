package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreateDrinkDetailMapper {

  boolean checkDrinkAccessible(
      @Param("drinkId") UUID drinkId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  int insertDrinkDetail(
      @Param("drinkDetailId") UUID drinkDetailId,
      @Param("drinkId") UUID drinkId,
      @Param("size") String size,
      @Param("price") java.math.BigDecimal price);
}
