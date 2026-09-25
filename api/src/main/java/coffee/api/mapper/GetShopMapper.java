package coffee.api.mapper;

import coffee.api.dto.result.ShopResult;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GetShopMapper {

  List<ShopResult> getShopsFiltered(
      @Param("search") String search,
      @Param("sortBy") String sortBy,
      @Param("sortDirection") String sortDirection,
      @Param("size") int size,
      @Param("offset") int offset,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserId") UUID currentUserId);

  long countShopsFiltered(
      @Param("search") String search,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserId") UUID currentUserId);
}
