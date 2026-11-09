package coffee.api.mapper;

import coffee.api.dto.result.InvoiceReceiptResult;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PrintInvoiceMapper {
  InvoiceReceiptResult findInvoiceForPrint(@Param("invoiceId") UUID invoiceId);
}
