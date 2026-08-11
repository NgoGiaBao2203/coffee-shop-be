package coffee.api.mapper;

import coffee.api.dto.result.RevenueResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GetRevenueMapper {

  List<RevenueResult> getRevenueFiltered(
      @Param("search") String search,
      @Param("shopId") UUID shopId,
      @Param("invoiceId") UUID invoiceId,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime,
      @Param("year") Integer year,
      @Param("month") Integer month,
      @Param("sortBy") String sortBy,
      @Param("sortDirection") String sortDirection,
      @Param("size") int size,
      @Param("offset") int offset,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  long countRevenueFiltered(
      @Param("search") String search,
      @Param("shopId") UUID shopId,
      @Param("invoiceId") UUID invoiceId,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime,
      @Param("year") Integer year,
      @Param("month") Integer month,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
