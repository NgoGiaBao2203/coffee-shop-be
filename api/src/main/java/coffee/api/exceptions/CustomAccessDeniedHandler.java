package coffee.api.exceptions;

import coffee.api.dto.response.base_response.ErrorApiResponse;
import coffee.api.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;

import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
  private final ObjectMapper objectMapper;

  public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(@NonNull HttpServletRequest request, HttpServletResponse response,
                     AccessDeniedException accessDeniedException) {

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json;charset=UTF-8");

    ErrorApiResponse body = ErrorApiResponse.of(ResponseCode.ACCESS_DENIED, "Access denied");

    log.warn("Access denied: {}", accessDeniedException.getMessage());

    try (var writer = response.getWriter()) {
      writer.write(objectMapper.writeValueAsString(body));
      writer.flush();
    } catch (IOException e) {
      log.error("Error writing response body", e);
    }
  }
}
