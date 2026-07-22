package coffee.api.exceptions;

import lombok.Getter;

@Getter
public class PhoneNumberExistedException extends RuntimeException{
  private final String phoneNumber;

  public PhoneNumberExistedException(String message, String phoneNumber) {
    super(message);
    this.phoneNumber = phoneNumber;
  }
}
