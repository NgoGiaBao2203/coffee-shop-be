package coffee.api.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class RevenueResult {
  private UUID invoiceId;
  private UUID shopId;
  private String shopName;
  private String fullName;
  private String drinkName;
  private LocalDateTime createdAt;
  private BigDecimal totalAmount;
  private Integer tableNumber;
  private String size;
  private Integer quantity;
  private BigDecimal price;
  private String note;
}
