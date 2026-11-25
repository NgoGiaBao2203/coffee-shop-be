package coffee.api.security;

import coffee.api.constants.MdcKey;
import coffee.api.dto.response.base_response.ErrorApiResponse;
import coffee.api.enums.ResponseCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class UserIdHeaderAuthenticationFilter extends OncePerRequestFilter {
  private static final String USER_ID_HEADER = "X-USER-ID";
  private final ICustomUserDetailsService customUserDetailsService;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String userIdHeader = request.getHeader(USER_ID_HEADER);

    if (userIdHeader != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UUID userId;
      try {
        userId = UUID.fromString(userIdHeader);
      } catch (IllegalArgumentException ex) {
        // Also covers NumberFormatException: a wrong length (e.g. "abc123") throws
        // IllegalArgumentException, wrong characters in a 36-char value throw NumberFormatException
        writeError(
            response,
            HttpServletResponse.SC_BAD_REQUEST,
            ResponseCode.BAD_REQUEST,
            "X-USER-ID header must be a valid UUID");
        return;
      }

      try {
        CustomUserDetail userDetail = customUserDetailsService.loadUserById(userId);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userDetail, null, userDetail.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Make userId available to every log statement in this request
        MDC.put(MdcKey.USER_ID, userId.toString());
      } catch (UsernameNotFoundException ex) {
        writeError(
            response,
            HttpServletResponse.SC_UNAUTHORIZED,
            ResponseCode.UNAUTHORIZED,
            "User not found for the provided X-USER-ID");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private void writeError(
      HttpServletResponse response, int status, ResponseCode code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    ErrorApiResponse body = ErrorApiResponse.of(code, message);
    try (var writer = response.getWriter()) {
      writer.write(objectMapper.writeValueAsString(body));
      writer.flush();
    }
  }
}
