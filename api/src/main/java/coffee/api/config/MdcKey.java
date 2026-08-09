package coffee.api.config;

public class MdcKey {
  public static final String TRACE_ID = "traceId";
  public static final String USER_ID = "userId";
  public static final String HTTP_METHOD = "method";
  public static final String PATH = "path";

  private MdcKey() {}
}
