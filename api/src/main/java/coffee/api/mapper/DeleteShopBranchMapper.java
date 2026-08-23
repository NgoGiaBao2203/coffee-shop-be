package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteShopBranchMapper {

  Boolean checkShopExistedById(@Param("shopId") UUID shopId);

  int countActiveInvoicesByShopId(@Param("shopId") UUID shopId);

  void softDeleteShopBranch(@Param("shopId") UUID shopId);

  void softDeleteCategoriesByShopId(@Param("shopId") UUID shopId);

  void softDeleteDrinksByShopId(@Param("shopId") UUID shopId);
}
