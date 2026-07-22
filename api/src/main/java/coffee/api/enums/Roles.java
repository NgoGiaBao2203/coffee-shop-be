package coffee.api.enums;

import lombok.Getter;

@Getter
public enum Roles {
  OWNER("OWNER"),
  MANAGER("MANAGER"),
  STAFF("STAFF");

  private final String value;

  Roles(String value) {
    this.value = value;
  }
}
