package coffee.api.mapper;

import java.math.BigDecimal;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateDrinkDetailMapper {

  int updateDrinkDetail(
      @Param("drinkDetailId") UUID drinkDetailId,
      @Param("size") String size,
      @Param("price") BigDecimal price,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
