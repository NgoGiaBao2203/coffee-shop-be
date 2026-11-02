package coffee.api.constants;

/** Exception messages shared by several services and validators. */
public final class ErrorMessage {

  // ── Generic ──────────────────────────────────────────────────────
  public static final String DATA_NOT_FOUND = "Data not found";
  public static final String INVALID_REQUEST = "Invalid request";

  // ── Required ids ─────────────────────────────────────────────────
  public static final String SHOP_ID_REQUIRED = "Shop ID is required";
  public static final String DRINK_ID_REQUIRED = "Drink ID is required";

  // ── Shop access ──────────────────────────────────────────────────
  public static final String SHOP_ACCESS_DENIED = "You do not have permission to access this shop";
  public static final String SHOP_BRANCH_ACCESS_DENIED =
      "You do not have permission to access this shop branch";
  public static final String USER_NOT_ASSIGNED_TO_SHOP = "User is not assigned to any shop";
  public static final String MANAGER_NOT_ASSIGNED_TO_SHOP_BRANCH =
      "Manager is not assigned to any shop branch";

  private ErrorMessage() {}
}
