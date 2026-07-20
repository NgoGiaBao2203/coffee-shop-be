package coffee.api.exceptions;

import coffee.api.dto.response.base_response.ErrorApiResponse;
import coffee.api.enums.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ObjectMapper objectMapper;

  public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(@NonNull HttpServletRequest request, HttpServletResponse response,
                       AuthenticationException authException) {

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");

    ErrorApiResponse body = ErrorApiResponse.of(ResponseCode.UNAUTHORIZED, "Unauthorized");

    log.warn("Unauthorized access: {}", authException.getMessage());

    try (var writer = response.getWriter()) {
      writer.write(objectMapper.writeValueAsString(body));
      writer.flush();
    } catch (IOException e) {
      log.error("Error writing response body", e);
    }
  }
}
