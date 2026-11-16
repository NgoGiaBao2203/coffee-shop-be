package coffee.api.constants;

/** Shared {@code @Pattern} regexes for request DTOs. */
public final class ValidationPattern {

  // ── Generic text ─────────────────────────────────────────────────
  // Unicode letters, numbers, spaces and punctuation: - & / ( ) , . '
  public static final String TEXT = "^[\\p{L}\\p{N}\\s\\-&/(),.']+$";
  // Same as TEXT, but blank is allowed (optional fields and search keywords)
  public static final String OPTIONAL_TEXT = "^\\s*$|" + TEXT;

  // ── Drink fields ─────────────────────────────────────────────────
  public static final String DRINK_SIZE = "^[SML]$";

  // ── Shop / Branch fields ─────────────────────────────────────────
  public static final String SHOP_NAME = "^[\\p{L}0-9\\s.,&'-]+$";
  public static final String SHOP_ADDRESS = "^[\\p{L}0-9\\s/.,#-]+$";
  // Shop name or address keyword; blank is allowed
  public static final String SHOP_SEARCH = "^\\s*$|^[\\p{L}0-9\\s.,/&'#-]+$";
  public static final String PHONE_DIGITS = "^\\s*$|^[0-9]+$";
  public static final String PHONE_PREFIX = "^\\s*$|^0.*$";
  public static final String PHONE_LENGTH = "^\\s*$|^.{10,11}$";

  private ValidationPattern() {}
}
