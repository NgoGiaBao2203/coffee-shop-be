package coffee.api.security;

import coffee.api.mapper.CustomUserMapper;
import coffee.api.model.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.UUID;


@Slf4j
@SuppressWarnings("ALL")
@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements ICustomUserDetailsService{
  private final CustomUserMapper customUserMapper;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserProfile user = customUserMapper.findByEmail(username);

    if (user == null) {
      throw new UsernameNotFoundException("User not found with email: " + username);
    }

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRoleName()));
    return new CustomUserDetail(
      user.getProfileId(),
      user.getEmail(),
      user.getPassword(),
      user.getIsDeleted(),
      user.getRoleName(),
      authorities
    );
  }

  @Override
  public CustomUserDetail loadUserById(UUID userId) {
    UserProfile user = customUserMapper.findByProfileId(userId);
    if (user == null) {
      throw new UsernameNotFoundException("User not found with id: " + userId);
    }

    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRoleName()));
    return new CustomUserDetail(
      user.getProfileId(),
      user.getEmail(),
      user.getPassword(),
      user.getIsDeleted(),
      user.getRoleName(),
      authorities
    );
  }
}
