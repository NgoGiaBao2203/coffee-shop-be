package coffee.api.exceptions;

public class InvalidUsernameOrPasswordException extends RuntimeException {
  public InvalidUsernameOrPasswordException(String message) {
    super(message);
  }
}
