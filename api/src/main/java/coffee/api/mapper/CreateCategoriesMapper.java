package coffee.api.mapper;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface CreateCategoriesMapper {
  void createCategories(
    @Param("request") CreateCategoriesRequest request,
    @Param("currentUserRoleName") String currentUserRoleName,
    @Param("currentUserShopId") UUID currentUserShopId
  );
}
