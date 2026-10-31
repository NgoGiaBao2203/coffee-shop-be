package coffee.api.mapper;

import coffee.api.dto.result.InvoiceResult;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateInvoiceMapper {
  InvoiceResult findInvoiceById(@Param("invoiceId") UUID invoiceId);

  UUID lockOccupiedTable(@Param("tableId") UUID tableId, @Param("shopId") UUID shopId);

  int updateInvoice(
      @Param("invoiceId") UUID invoiceId,
      @Param("tableId") UUID tableId,
      @Param("totalAmount") BigDecimal totalAmount,
      @Param("shopId") UUID shopId);

  int releaseTable(@Param("tableId") UUID tableId, @Param("shopId") UUID shopId);

  int deleteInvoiceDetails(@Param("invoiceId") UUID invoiceId);
}
