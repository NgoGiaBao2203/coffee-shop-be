package coffee.api.exceptions;

public class UserExistException extends RuntimeException {
  public UserExistException(String message) {
    super(message);
  }
}
