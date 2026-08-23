package coffee.api.mapper;

import coffee.api.dto.request.invoice.CreateInvoiceItemRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreateInvoiceMapper {

  int insertInvoice(
      @Param("invoiceId") UUID invoiceId,
      @Param("shopId") UUID shopId,
      @Param("profileId") UUID profileId,
      @Param("tableNumber") Integer tableNumber,
      @Param("totalAmount") BigDecimal totalAmount,
      @Param("status") Integer status);

  int insertInvoiceDetails(
      @Param("invoiceId") UUID invoiceId, @Param("items") List<CreateInvoiceItemRequest> items);
}
