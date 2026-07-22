package coffee.api.exceptions;

import lombok.Getter;

@Getter
public class EmailExistedException extends RuntimeException {
  private final String email;

  public EmailExistedException(String message, String email) {
    super(message);
    this.email = email;
  }
}
