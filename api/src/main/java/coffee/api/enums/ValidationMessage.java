package coffee.api.enums;

public enum ValidationMessage {
  // ── Generic ──────────────────────────────────────────────────────
  FIELD_REQUIRED(Msg.FIELD_REQUIRED),
  SPECIAL_CHARACTERS(Msg.SPECIAL_CHARACTERS),

  // ── User fields ──────────────────────────────────────────────────
  PASSWORD_MIN_LENGTH(Msg.PASSWORD_MIN_LENGTH),

  // ── Shop / Branch fields ─────────────────────────────────────────
  PHONE_NUMBER_INVALID(Msg.PHONE_NUMBER_INVALID),
  PHONE_INVALID_LENGTH(Msg.PHONE_INVALID_LENGTH),

  // ── Pagination fields ────────────────────────────────────────────
  PAGE_MIN(Msg.PAGE_MIN),
  SIZE_MIN(Msg.SIZE_MIN),
  SIZE_MAX(Msg.SIZE_MAX),

  // ── Sort fields ──────────────────────────────────────────────────
  SORT_BY_INVALID(Msg.SORT_BY_INVALID);

  private final String code;

  ValidationMessage(String code) {
    this.code = code;
  }

  public static final class Msg {

    public static final String FIELD_REQUIRED = "EV001";
    public static final String PASSWORD_MIN_LENGTH = "EV002";
    public static final String PAGE_MIN = "EV003";
    public static final String SIZE_MIN = "EV004";
    public static final String SIZE_MAX = "EV005";
    public static final String SORT_BY_INVALID = "EV006";
    public static final String SPECIAL_CHARACTERS = "EV007";
    public static final String PHONE_NUMBER_INVALID = "EV008";
    public static final String PHONE_INVALID_LENGTH = "EV009";

    private Msg(String code) {}

    private Msg() {}
  }
}
