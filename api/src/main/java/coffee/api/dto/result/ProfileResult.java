package coffee.api.dto.result;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class ProfileResult {
  private UUID profileId;
  private String email;
  private String username;
  private String fullName;
  private String phoneNumber;
  private String shopName;
  private String roleName;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Boolean isDeleted;
}
