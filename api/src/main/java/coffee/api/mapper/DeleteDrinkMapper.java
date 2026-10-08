package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteDrinkMapper {
  int deleteDrink(
      @Param("drinkId") UUID drinkId,
      @Param("deletedBy") UUID deletedBy,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
