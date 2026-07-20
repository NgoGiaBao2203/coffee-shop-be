package coffee.api.enums;

public enum ValidationMessage {
  // ── Generic ──────────────────────────────────────────────────────
  FIELD_REQUIRED(Msg.FIELD_REQUIRED),

  // ── User fields ──────────────────────────────────────────────────
  PASSWORD_MIN_LENGTH(Msg.PASSWORD_MIN_LENGTH),

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

    private Msg() {
    }
  }
}
