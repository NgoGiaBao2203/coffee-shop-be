package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreateTablesMapper {
  Boolean checkTableNumberExisted(
      @Param("shopId") UUID shopId, @Param("tableNumber") Integer tableNumber);

  void createTable(
      @Param("tableNumber") Integer tableNumber,
      @Param("description") String description,
      @Param("status") Integer status,
      @Param("shopId") UUID shopId);
}
