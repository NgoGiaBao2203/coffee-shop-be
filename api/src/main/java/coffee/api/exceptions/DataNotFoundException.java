package coffee.api.exceptions;

import lombok.Getter;

import java.util.UUID;

@Getter
public class DataNotFoundException extends RuntimeException {
  private final UUID id;

  public DataNotFoundException(String message, UUID id) {
    super(message);
    this.id = id;
  }}
