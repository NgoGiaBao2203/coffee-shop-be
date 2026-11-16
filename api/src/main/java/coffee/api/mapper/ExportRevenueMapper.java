package coffee.api.mapper;

import coffee.api.dto.result.RevenueResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExportRevenueMapper {

  List<RevenueResult> getRevenueForExport(
      @Param("search") String search,
      @Param("shopId") UUID shopId,
      @Param("invoiceId") UUID invoiceId,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime,
      @Param("sortBy") String sortBy,
      @Param("sortDirection") String sortDirection,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  String getShopName(@Param("shopId") UUID shopId);

  String getProfileName(@Param("profileId") UUID profileId);
}
