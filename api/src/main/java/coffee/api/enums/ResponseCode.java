package coffee.api.enums;

import lombok.Getter;

@Getter
public enum ResponseCode {
  // ── Success ──────────────────────────────────────────────────────
  SUCCESS("S0001"),

  // ── Client errors ────────────────────────────────────────────────
  BAD_REQUEST("ER001"),
  UNAUTHORIZED("ER002"),
  ACCESS_DENIED("ER003"),
  NOT_FOUND("ER004"),
  CONFLICT("ER005"),
  METHOD_NOT_ALLOWED("ER006"),
  UNSUPPORTED_MEDIA_TYPE("ER007"),
  INVALID_REQUEST("ER008"),
  USERNAME_OR_PASSWORD_INCORRECT("ER009"),
  ACCOUNT_DISABLE("ER010"),
  // ── Server errors ────────────────────────────────────────────────
  INTERNAL_SERVER_ERROR("ER500");

  private final String code;
  ResponseCode(String code) {
    this.code = code;
  }
}
