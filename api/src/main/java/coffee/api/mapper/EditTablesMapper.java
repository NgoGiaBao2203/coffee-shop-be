package coffee.api.mapper;

import coffee.api.dto.request.table.EditTablesRequest;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EditTablesMapper {
  void updateTable(
      @Param("request") EditTablesRequest request,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
