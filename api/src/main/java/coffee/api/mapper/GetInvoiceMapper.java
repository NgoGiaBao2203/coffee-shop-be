package coffee.api.mapper;

import coffee.api.dto.result.InvoiceResult;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GetInvoiceMapper {

  List<InvoiceResult> getInvoicesFiltered(
      @Param("search") String search,
      @Param("sortBy") String sortBy,
      @Param("sortDirection") String sortDirection,
      @Param("size") int size,
      @Param("offset") int offset,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId,
      @Param("shopId") UUID shopId,
      @Param("status") Integer status);

  long countInvoicesFiltered(
      @Param("search") String search,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId,
      @Param("shopId") UUID shopId,
      @Param("status") Integer status);
}
