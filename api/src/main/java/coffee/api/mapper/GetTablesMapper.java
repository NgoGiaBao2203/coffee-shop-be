package coffee.api.mapper;

import coffee.api.dto.result.TableResult;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GetTablesMapper {
  List<TableResult> getTablesFiltered(
      @Param("search") String search,
      @Param("status") Integer status,
      @Param("sortBy") String sortBy,
      @Param("sortDirection") String sortDirection,
      @Param("size") int size,
      @Param("offset") int offset,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserId") UUID currentUserId);

  long countTablesFiltered(
      @Param("search") String search,
      @Param("status") Integer status,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserId") UUID currentUserId);
}
