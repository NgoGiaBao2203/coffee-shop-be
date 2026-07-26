package coffee.api.mapper;

import coffee.api.dto.result.DrinkResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface GetDrinksMapper {
  List<DrinkResult> getDrinksFiltered(
    @Param("search") String search,
    @Param("sortBy") String sortBy,
    @Param("sortDirection") String sortDirection,
    @Param("size") int size,
    @Param("offset") int offset,
    @Param("currentUserRoleName") String currentUserRoleName,
    @Param("currentUserId") UUID currentUserId
  );

  long countDrinksFiltered(
    @Param("search") String search,
    @Param("currentUserRoleName") String currentUserRoleName,
    @Param("currentUserId") UUID currentUserId
  );
}
