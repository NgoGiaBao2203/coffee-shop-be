package coffee.api.mapper;

import coffee.api.dto.request.invoice.EditInvoiceItemRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateInvoiceMapper {

  int updateInvoice(
      @Param("invoiceId") UUID invoiceId,
      @Param("tableNumber") Integer tableNumber,
      @Param("totalAmount") BigDecimal totalAmount,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);

  int deleteInvoiceDetails(@Param("invoiceId") UUID invoiceId);

  int batchInsertInvoiceDetails(
      @Param("invoiceId") UUID invoiceId, @Param("items") List<EditInvoiceItemRequest> items);
}
