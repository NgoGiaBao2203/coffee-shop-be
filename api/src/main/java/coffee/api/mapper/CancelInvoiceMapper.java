package coffee.api.mapper;

import coffee.api.dto.result.InvoiceResult;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CancelInvoiceMapper {

  InvoiceResult findInvoiceById(@Param("invoiceId") UUID invoiceId);

  int cancelInvoice(
      @Param("invoiceId") UUID invoiceId,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
