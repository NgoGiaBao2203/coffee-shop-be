package coffee.api.exceptions;

import java.util.UUID;
import lombok.Getter;

@Getter
public class DataNotFoundException extends RuntimeException {
  private final UUID id;

  public DataNotFoundException(String message, UUID id) {
    super(message);
    this.id = id;
  }
}
