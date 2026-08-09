package coffee.api.utils;

import coffee.api.enums.Roles;

public class ConvertRoleVN {
  /**
   * Converts a Roles enum to its Vietnamese display name. Returns "KÍCH THƯỚC/VAI TRÒ KHÔNG XÁC
   * ĐỊNH" or empty string if null.
   */
  public static String toVietnamese(Roles role) {
    if (role == null) {
      return "";
    }

    return switch (role) {
      case OWNER -> "CHỦ QUÁN";
      case MANAGER -> "QUẢN LÝ";
      case STAFF -> "NHÂN VIÊN";
    };
  }

  /** Overload method to accept a raw String value (e.g., "OWNER") */
  public static String toVietnamese(String roleValue) {
    try {
      return toVietnamese(Roles.valueOf(roleValue.toUpperCase()));
    } catch (IllegalArgumentException | NullPointerException e) {
      return "KHÔNG XÁC ĐỊNH";
    }
  }
}
