package coffee.api.config;

import coffee.api.exceptions.CustomAccessDeniedHandler;
import coffee.api.exceptions.CustomAuthenticationEntryPoint;
import coffee.api.security.ICustomUserDetailsService;
import coffee.api.security.UserIdHeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final ICustomUserDetailsService customUserDetailsService;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final ObjectMapper objectMapper;

  @Bean
  public UserIdHeaderAuthenticationFilter userIdHeaderAuthenticationFilter() {
    return new UserIdHeaderAuthenticationFilter(customUserDetailsService, objectMapper);
  }

  @Bean
  public FilterRegistrationBean<UserIdHeaderAuthenticationFilter> userIdFilterRegistration() {
    FilterRegistrationBean<UserIdHeaderAuthenticationFilter> registration =
      new FilterRegistrationBean<>(userIdHeaderAuthenticationFilter());
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) {
    return http
      .csrf(AbstractHttpConfigurer::disable)
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/common/**", "/error").permitAll()
        .anyRequest().authenticated()
      )
      .addFilterBefore(userIdHeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
      .exceptionHandling(ex -> ex
        .accessDeniedHandler(customAccessDeniedHandler)
        .authenticationEntryPoint(customAuthenticationEntryPoint)
      )
      .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
