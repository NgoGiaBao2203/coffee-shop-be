package coffee.api.exceptions;

import lombok.Getter;

@Getter
public class AccountDisableException extends RuntimeException {
  private final String username;

  public AccountDisableException(String message, String username) {
    super(message);
    this.username = username;
  }
}
