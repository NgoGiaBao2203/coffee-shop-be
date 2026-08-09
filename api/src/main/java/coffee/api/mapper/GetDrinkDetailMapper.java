package coffee.api.mapper;

import coffee.api.dto.result.DrinkResult;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GetDrinkDetailMapper {
  DrinkResult getDrinkDetailById(
      @Param("drinkId") UUID drinkId,
      @Param("currentUserShopId") UUID currentUserShopId,
      @Param("currentUserRoleName") String currentUserRoleName);
}
