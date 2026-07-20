package coffee.api.utils;

import coffee.api.config.MdcKey;
import org.slf4j.MDC;

import java.util.UUID;

public final class MdcUtil {
  private MdcUtil() {
  }

  public static String getTraceId() {
    String traceId = MDC.get(MdcKey.TRACE_ID);
    return (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
  }

  public static String getUserId() {
    String userId = MDC.get(MdcKey.USER_ID);
    return (userId != null && !userId.isBlank()) ? userId : "anonymous";
  }
}
