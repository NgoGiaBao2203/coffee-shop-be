package coffee.api.dto.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleResult {
  private UUID roleId;
  @JsonIgnore private String roleName;
  private String roleDisplayName;
}
