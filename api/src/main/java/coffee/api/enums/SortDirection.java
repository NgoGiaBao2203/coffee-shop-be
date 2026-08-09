package coffee.api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SortDirection {
  ASC,
  DESC;

  @JsonCreator
  public static SortDirection from(String value) {
    if (value == null) return ASC;
    return switch (value.toUpperCase()) {
      case "DESC" -> DESC;
      default -> ASC;
    };
  }

  @JsonValue
  public String getValue() {
    return this.name();
  }
}
