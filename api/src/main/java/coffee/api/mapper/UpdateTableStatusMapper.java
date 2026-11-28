package coffee.api.mapper;

import coffee.api.dto.result.TableResult;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateTableStatusMapper {

  TableResult lockTable(
      @Param("tableId") UUID tableId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  int updateTableStatus(
      @Param("tableId") UUID tableId,
      @Param("shopId") UUID shopId,
      @Param("status") Integer status);
}
