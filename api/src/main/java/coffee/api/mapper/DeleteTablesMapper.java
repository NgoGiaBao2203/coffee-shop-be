package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteTablesMapper {
  void deleteTable(
      @Param("tableId") UUID tableId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
