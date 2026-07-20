package coffee.api.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserProfile {
  private UUID profileId;
  private String email;
  private String username;
  private String password;
  private String fullName;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String phoneNumber;
  private String roleName;
  private Boolean isDeleted;
}
