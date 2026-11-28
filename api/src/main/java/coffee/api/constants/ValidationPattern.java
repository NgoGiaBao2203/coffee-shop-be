package coffee.api.constants;

/** Shared {@code @Pattern} regexes for request DTOs. */
public final class ValidationPattern {

  // ── Generic ──────────────────────────────────────────────────────
  // Required. Allowed: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
  public static final String TEXT = "^[\\p{L}\\p{N}\\s\\-&/(),.']+$";
  // Same as TEXT, but blank is allowed (optional fields and search keywords)
  public static final String OPTIONAL_TEXT = "^\\s*$|" + TEXT;
  // Blank is allowed. Allowed: digits 0-9 only
  public static final String PHONE_DIGITS = "^\\s*$|^[0-9]+$";
  // Blank is allowed. Allowed: strings starting with '0'
  public static final String PHONE_PREFIX = "^\\s*$|^0.*$";
  // Blank is allowed. Allowed: 10 to 11 characters in length
  public static final String PHONE_LENGTH = "^\\s*$|^.{10,11}$";

  // ── Drink fields ─────────────────────────────────────────────────
  // Required. Allowed: single uppercase letter 'S', 'M', or 'L'
  public static final String DRINK_SIZE = "^[SML]$";

  // ── Shop / Branch fields ─────────────────────────────────────────
  // Required. Allowed: Unicode letters, digits (0-9), spaces, and punctuation: . , & ' -
  public static final String SHOP_NAME = "^[\\p{L}0-9\\s.,&'-]+$";
  // Required. Allowed: Unicode letters, digits (0-9), spaces, and punctuation: / . , # -
  public static final String SHOP_ADDRESS = "^[\\p{L}0-9\\s/.,#-]+$";
  // Blank is allowed. Allowed: Unicode letters, digits (0-9), spaces, and punctuation: . , / & ' #
  // -
  public static final String SHOP_SEARCH = "^\\s*$|^[\\p{L}0-9\\s.,/&'#-]+$";

  // ── Staff / account fields ───────────────────────────────────────
  // Required. Allowed: ASCII letters (A-Z, a-z), digits (0-9), and symbols: . _ -
  public static final String USERNAME = "^[A-Za-z0-9._-]+$";
  // Blank is allowed. Allowed: Unicode letters and numbers, spaces, and . , / & @ _ # -
  // Not allowed: ( ) ' + and any other symbol
  public static final String STAFF_SEARCH = "^[\\p{L}\\p{N}\\s.,/&@_#-]*$";
  // Blank is allowed (left to @NotBlank). Not allowed: any whitespace (leading, trailing, inside)
  public static final String PASSWORD_NO_WHITESPACE = "^\\S*$";
  // Blank is allowed. Allowed: printable ASCII only (no Vietnamese accents, no emoji), so one
  // character is one byte and @Size(max = 72) also keeps BCrypt within its 72-byte limit.
  // Whitespace is left to PASSWORD_NO_WHITESPACE so it is not reported twice.
  public static final String PASSWORD_ASCII = "^[\\x21-\\x7E\\s]*$";
  // Blank is allowed (left to @NotBlank). Required: at least one lowercase, one uppercase,
  // one digit and one ASCII special character: ! " # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \ ]
  // ^ _ ` { | } ~
  public static final String PASSWORD_STRONG =
      "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\p{Punct}).*$";

  private ValidationPattern() {}
}
