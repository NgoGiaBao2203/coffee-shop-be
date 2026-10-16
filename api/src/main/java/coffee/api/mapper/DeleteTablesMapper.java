package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteTablesMapper {

  int countPendingInvoicesByTable(@Param("tableId") UUID tableId);

  int softDeleteTable(
      @Param("tableId") UUID tableId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId,
      @Param("deletedBy") UUID deletedBy);
}
