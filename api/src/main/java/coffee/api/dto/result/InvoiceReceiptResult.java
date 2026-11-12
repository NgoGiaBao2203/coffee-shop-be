package coffee.api.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

/** One invoice with its shop header and drink lines, as printed on the receipt. */
@Data
public class InvoiceReceiptResult {
  private UUID invoiceId;
  private String invoiceCode;
  private UUID shopId;
  private String shopName;
  private String shopAddress;
  private String shopPhone;
  private String staffName;
  private Integer tableNumber;
  private BigDecimal totalAmount;
  private Integer status;
  private LocalDateTime createdAt;
  private List<InvoiceResult.InvoiceItemResult> items;
}
