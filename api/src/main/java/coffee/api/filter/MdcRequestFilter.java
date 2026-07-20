package coffee.api.filter;

import coffee.api.config.MdcKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcRequestFilter extends OncePerRequestFilter {
  private static final String TRACE_ID_HEADER = "X-Trace-Id";

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
    try {
      String traceId = resolveTraceId(request);
      MDC.put(MdcKey.TRACE_ID, traceId);
      MDC.put(MdcKey.HTTP_METHOD, request.getMethod());
      MDC.put(MdcKey.PATH, request.getRequestURI());
      response.setHeader(TRACE_ID_HEADER, traceId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  private String resolveTraceId(HttpServletRequest request) {
    String incoming = request.getHeader(TRACE_ID_HEADER);
    return (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();
  }
}
