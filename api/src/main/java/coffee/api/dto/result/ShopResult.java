package coffee.api.dto.result;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class ShopResult {
  private UUID shopId;
  private String shopName;
  private String address;
  private String phoneNumber;
  private Integer activeStaffCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Boolean isDeleted;
}
