package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteCategoryMapper {
  void deleteCategory(
      @Param("categoryId") UUID categoryId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
