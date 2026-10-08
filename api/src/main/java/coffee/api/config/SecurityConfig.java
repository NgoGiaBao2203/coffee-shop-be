package coffee.api.config;

import coffee.api.exceptions.CustomAccessDeniedHandler;
import coffee.api.exceptions.CustomAuthenticationEntryPoint;
import coffee.api.security.ICustomUserDetailsService;
import coffee.api.security.UserIdHeaderAuthenticationFilter;
import java.util.List;
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

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final ICustomUserDetailsService customUserDetailsService;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final ObjectMapper objectMapper;
  private final CorsProperties corsProperties;

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
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/common/**", "/error", "/uploads/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            userIdHeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(customAccessDeniedHandler)
                    .authenticationEntryPoint(customAuthenticationEntryPoint))
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // Use allowedOriginPatterns when credentials are enabled or patterns are provided
    if (corsProperties.isAllowCredentials()
        && corsProperties.getAllowedOriginPatterns() != null
        && !corsProperties.getAllowedOriginPatterns().isEmpty()) {
      config.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
    } else if (corsProperties.getAllowedOrigins() != null
        && !corsProperties.getAllowedOrigins().isEmpty()) {
      config.setAllowedOrigins(corsProperties.getAllowedOrigins());
    } else {
      // Fallback to localhost for development
      config.setAllowedOrigins(List.of("http://localhost:5173"));
    }

    if (corsProperties.getAllowedMethods() != null
        && !corsProperties.getAllowedMethods().isEmpty()) {
      config.setAllowedMethods(corsProperties.getAllowedMethods());
    } else {
      config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    }

    if (corsProperties.getAllowedHeaders() != null
        && !corsProperties.getAllowedHeaders().isEmpty()) {
      config.setAllowedHeaders(corsProperties.getAllowedHeaders());
    } else {
      config.setAllowedHeaders(List.of("*"));
    }

    config.setAllowCredentials(corsProperties.isAllowCredentials());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
