package coffee.api.mapper;

import coffee.api.dto.request.invoice.EditInvoiceItemRequest;
import coffee.api.dto.result.InvoiceResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateInvoiceMapper {

  InvoiceResult findInvoiceById(@Param("invoiceId") UUID invoiceId);

  boolean isInvoiceCreatedBy(
      @Param("invoiceId") UUID invoiceId, @Param("profileId") UUID profileId);

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
