package coffee.api.mapper;

import coffee.api.dto.result.InvoiceResult;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PayInvoiceMapper {

  InvoiceResult findInvoiceById(@Param("invoiceId") UUID invoiceId);

  int payInvoice(
      @Param("invoiceId") UUID invoiceId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
