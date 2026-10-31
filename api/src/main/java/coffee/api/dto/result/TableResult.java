package coffee.api.dto.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableResult {
  private UUID tableId;
  private UUID shopId;
  private String shopName;
  private Integer tableNumber;
  private String description;
  private Integer status;
  private String statusName;

  // Audit timestamps - strictly nullified for STAFF
  private String createdAt;
  private String updatedAt;
}
