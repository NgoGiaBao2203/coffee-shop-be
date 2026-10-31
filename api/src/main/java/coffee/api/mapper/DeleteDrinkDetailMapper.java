package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteDrinkDetailMapper {

  int countActiveDrinkDetails(@Param("drinkId") UUID drinkId);

  int softDeleteDrinkDetail(
      @Param("drinkDetailId") UUID drinkDetailId,
      @Param("deletedBy") UUID deletedBy,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
