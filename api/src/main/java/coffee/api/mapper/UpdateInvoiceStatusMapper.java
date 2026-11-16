package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateInvoiceStatusMapper {
  int updateInvoiceStatus(
      @Param("invoiceId") UUID invoiceId,
      @Param("shopId") UUID shopId,
      @Param("status") Integer status);
}
