package coffee.api.mapper;

import coffee.api.dto.result.DrinkResult;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GetDrinksMapper {
  List<DrinkResult> getDrinksFiltered(
      @Param("search") String search,
      @Param("sortBy") String sortBy,
      @Param("sortDirection") String sortDirection,
      @Param("size") int size,
      @Param("offset") int offset,
      @Param("currentUserShopId") UUID currentUserShopId,
      @Param("currentUserRoleName") String currentUserRoleName);

  long countDrinksFiltered(
      @Param("search") String search,
      @Param("currentUserShopId") UUID currentUserShopId,
      @Param("currentUserRoleName") String currentUserRoleName);
}
