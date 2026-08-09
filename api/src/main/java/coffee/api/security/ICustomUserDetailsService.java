package coffee.api.security;

import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface ICustomUserDetailsService extends UserDetailsService {
  CustomUserDetail loadUserById(UUID userId);
}
