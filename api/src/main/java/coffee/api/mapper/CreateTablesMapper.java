package coffee.api.mapper;

import coffee.api.dto.request.table.CreateTablesRequest;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreateTablesMapper {

  void createTable(
      @Param("request") CreateTablesRequest request,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
