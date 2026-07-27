package coffee.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface DeleteCategoryMapper {
  void deleteCategory(
    @Param("categoryId") UUID categoryId,
    @Param("currentUserRoleName") String currentUserRoleName,
    @Param("currentUserShopId") UUID currentUserShopId
  );
}
