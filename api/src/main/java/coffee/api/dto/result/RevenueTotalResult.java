package coffee.api.dto.result;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RevenueTotalResult {
  private BigDecimal totalRevenue;
  private Long totalInvoices;
}
