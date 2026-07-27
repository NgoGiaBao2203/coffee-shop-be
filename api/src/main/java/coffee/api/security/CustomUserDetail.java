package coffee.api.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

@Getter
@SuppressWarnings("ALL")
public class CustomUserDetail implements UserDetails {
  private final UUID userId;
  private final String email;
  private final String password;
  private final String roleName;
  private final UUID shopId;
  private final Boolean active;
  private final Collection<? extends GrantedAuthority> authorities;

  public CustomUserDetail(UUID userId, String email, String password, Boolean active, String roleName, UUID shopId, Collection<? extends GrantedAuthority> authorities) {
    this.userId = userId;
    this.email = email;
    this.password = password;
    this.active = active;
    this.roleName = roleName;
    this.shopId = shopId;
    this.authorities = authorities;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email; // Spring uses this as the principal username
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return Boolean.TRUE.equals(active);
  }
}
