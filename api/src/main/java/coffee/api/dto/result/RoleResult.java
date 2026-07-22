package coffee.api.dto.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleResult {
  private UUID roleId;
  @JsonIgnore
  private String roleName;
  private String roleDisplayName;
}