package coffee.api.mapper;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreateCategoriesMapper {

  void createCategories(
      @Param("request") CreateCategoriesRequest request,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
