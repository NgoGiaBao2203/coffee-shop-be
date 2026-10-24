package coffee.api.dto.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableResult {
  @JsonProperty("table_id")
  private UUID tableId;

  @JsonProperty("table_number")
  private Integer tableNumber;

  @JsonProperty("description")
  private String description;

  @JsonProperty("status")
  private Integer status;

  @JsonProperty("status_name")
  private String statusName;
}
