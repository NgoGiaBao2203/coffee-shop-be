package coffee.api.exceptions;

import coffee.api.dto.response.base_response.ErrorDetail;
import lombok.Getter;

import java.util.List;

@Getter
public class InvalidRequestWithErrorDetailsException extends RuntimeException {
  private final List<ErrorDetail> errorDetails;

  public InvalidRequestWithErrorDetailsException(String message, List<ErrorDetail> errorDetails) {
    super(message);
    this.errorDetails = errorDetails;
  }
}
