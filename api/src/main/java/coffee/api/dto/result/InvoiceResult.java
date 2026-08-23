package coffee.api.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class InvoiceResult {

  private UUID invoiceId;
  private UUID shopId;
  private String shopName;
  private String staffName;
  private Integer tableNumber;
  private BigDecimal totalAmount;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updateAt;
  private Boolean isDeleted;

  private List<InvoiceItemResult> items;

  @Data
  public static class InvoiceItemResult {
    private UUID invoiceDetailId;
    private UUID drinkId;
    private String drinkName;
    private String size;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal itemTotal;
    private String note;
  }
}
