package coffee.api.mapper;

import coffee.api.dto.request.category.EditCategoriesRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface UpdateCategoryMapper {
  void updateCategory(
    @Param("request")EditCategoriesRequest request,
    @Param("currentUserRoleName") String currentUserRoleName,
    @Param("currentUserShopId") UUID currentUserShopId
  );
}
