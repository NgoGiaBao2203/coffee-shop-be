package coffee.api.constants;

/** Shared {@code @PreAuthorize} expressions for controllers. */
public final class Authority {

  public static final String OWNER = "hasRole('OWNER')";
  public static final String OWNER_OR_MANAGER = "hasAnyRole('OWNER', 'MANAGER')";
  public static final String ANY_ROLE = "hasAnyRole('OWNER', 'MANAGER', 'STAFF')";

  private Authority() {}
}
