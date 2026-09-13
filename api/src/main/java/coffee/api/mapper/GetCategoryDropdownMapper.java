package coffee.api.mapper;

import coffee.api.dto.result.CategoryDropdownResult;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GetCategoryDropdownMapper {
  List<CategoryDropdownResult> getCategoryDropdown(
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
