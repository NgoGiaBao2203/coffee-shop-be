package coffee.api.mapper;

import coffee.api.dto.result.RevenueResult;
import java.math.BigDecimal;
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
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  BigDecimal getRevenueTotal(
      @Param("search") String search,
      @Param("shopId") UUID shopId,
      @Param("invoiceId") UUID invoiceId,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
