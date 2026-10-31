package coffee.api.mapper;

import coffee.api.dto.request.invoice.InvoiceItemRequest;
import coffee.api.dto.result.DrinkDetailPriceResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreateInvoiceMapper {

  List<DrinkDetailPriceResult> getSellableDrinkPrices(
      @Param("drinkDetailIds") Set<UUID> drinkDetailIds, @Param("shopId") UUID shopId);

  boolean checkTableAvailableInShop(@Param("tableId") UUID tableId, @Param("shopId") UUID shopId);

  int occupyTable(@Param("tableId") UUID tableId, @Param("shopId") UUID shopId);

  void insertInvoice(
      @Param("invoiceId") UUID invoiceId,
      @Param("shopId") UUID shopId,
      @Param("profileId") UUID profileId,
      @Param("tableId") UUID tableId,
      @Param("totalAmount") BigDecimal totalAmount,
      @Param("status") Integer status);

  int insertInvoiceDetails(
      @Param("invoiceId") UUID invoiceId, @Param("items") List<InvoiceItemRequest> items);
}
