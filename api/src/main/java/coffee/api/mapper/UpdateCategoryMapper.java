package coffee.api.mapper;

import coffee.api.dto.request.category.EditCategoriesRequest;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateCategoryMapper {
  void updateCategory(
      @Param("request") EditCategoriesRequest request,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
