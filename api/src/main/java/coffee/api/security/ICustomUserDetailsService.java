package coffee.api.security;

import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

public interface ICustomUserDetailsService extends UserDetailsService {
  CustomUserDetail loadUserById(UUID userId);
}
