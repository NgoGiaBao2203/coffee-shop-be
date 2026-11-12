package coffee.api.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class RevenueResult {

  private UUID invoiceId;
  private String invoiceCode;
  private UUID shopId;
  private String shopName;
  private String staffName;
  private Integer tableNumber;
  private BigDecimal totalAmount;
  private LocalDateTime createdAt;

  private List<RevenueItemResult> items;

  @Data
  public static class RevenueItemResult {
    private UUID invoiceDetailId;
    private UUID drinkDetailId;
    private UUID drinkId;
    private String drinkName;
    private String size;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal itemTotal;
    private String note;
  }
}
