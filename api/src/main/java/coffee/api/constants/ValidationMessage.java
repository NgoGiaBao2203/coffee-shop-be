package coffee.api.constants;

/** Validation error codes for request DTO annotations (the {@code message} attribute). */
public final class ValidationMessage {

  public static final String FIELD_REQUIRED = "EV001";
  public static final String PASSWORD_MIN_LENGTH = "EV002";
  public static final String PAGE_MIN = "EV003";
  public static final String SIZE_MIN = "EV004";
  public static final String SIZE_MAX = "EV005";
  public static final String SORT_BY_INVALID = "EV006";
  public static final String SPECIAL_CHARACTERS = "EV007";
  public static final String PHONE_NUMBER_INVALID = "EV008";
  public static final String PHONE_INVALID_LENGTH = "EV009";
  public static final String PRICE_MIN = "EV010";
  public static final String STATUS_INVALID = "EV011";

  private ValidationMessage() {}
}
