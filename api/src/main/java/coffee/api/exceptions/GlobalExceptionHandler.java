package coffee.api.exceptions;

import coffee.api.dto.response.base_response.ErrorApiResponse;
import coffee.api.dto.response.base_response.ErrorDetail;
import coffee.api.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorApiResponse> handleValidationException(MethodArgumentNotValidException ex) {
    List<ErrorDetail> errorDetails = ex.getBindingResult().getFieldErrors().stream()
      .map(fieldError -> ErrorDetail.builder()
        .field(fieldError.getField())
        .message(fieldError.getDefaultMessage())
        .build())
      .toList();
    log.error("Validation failed: {}", errorDetails);
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorApiResponse.of(ResponseCode.INVALID_REQUEST, "Invalid request", errorDetails));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorApiResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
    log.error("Malformed request body: {}", ex.getMessage());
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorApiResponse.of(ResponseCode.BAD_REQUEST, "Malformed or unreadable request body"));
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorApiResponse> handleNoHandlerFound(NoHandlerFoundException ex) {
    String message = String.format("No endpoint [%s] %s", ex.getHttpMethod(), ex.getRequestURL());
    log.error(message);
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorApiResponse.of(ResponseCode.NOT_FOUND, message));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorApiResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
    String allowed = ex.getSupportedHttpMethods() != null
      ? ex.getSupportedHttpMethods().stream()
      .map(HttpMethod::name)
      .collect(Collectors.joining(", "))
      : "unknown";
    String message = String.format(
      "HTTP method [%s] is not supported for this endpoint. Allowed: [%s]",
      ex.getMethod(), allowed
    );
    log.error(message);
    return ResponseEntity
      .status(HttpStatus.METHOD_NOT_ALLOWED)
      .body(ErrorApiResponse.of(ResponseCode.METHOD_NOT_ALLOWED, message));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorApiResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
    String message = String.format(
      "Content type [%s] is not supported. Please use [application/json]",
      ex.getContentType()
    );
    log.error(message);
    return ResponseEntity
      .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
      .body(ErrorApiResponse.of(ResponseCode.UNSUPPORTED_MEDIA_TYPE, message));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorApiResponse> handleResponseStatusException(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    ResponseCode code = (status == HttpStatus.UNAUTHORIZED)
      ? ResponseCode.UNAUTHORIZED
      : ResponseCode.BAD_REQUEST;
    log.error("Response Status Exception: {}", ex.getReason());
    return ResponseEntity
      .status(ex.getStatusCode())
      .body(ErrorApiResponse.of(code, ex.getReason() != null ? ex.getReason() : ex.getMessage()));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ErrorApiResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
    log.warn("Authorization Denied Exception: {}", ex.getMessage());
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(ErrorApiResponse.of(ResponseCode.ACCESS_DENIED, "Access denied"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorApiResponse> handleGenericException(Exception ex) {
    log.error("Server Internal Error Exception", ex);
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ErrorApiResponse.of(ResponseCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
  }

  @ExceptionHandler(UserExistException.class)
  public ResponseEntity<ErrorApiResponse> handleUserExistException(UserExistException ex) {
    log.error("User Exist Exception: {}", ex.getMessage());
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ErrorApiResponse.of(ResponseCode.CONFLICT, ex.getMessage()));
  }

  @ExceptionHandler(InvalidUsernameOrPasswordException.class)
  public ResponseEntity<ErrorApiResponse> handleUsernameNotFoundException(
    InvalidUsernameOrPasswordException ex
  ) {
    log.error("Username or Password Incorrect Exception: {}", ex.getMessage());
    return ResponseEntity
      .status(HttpStatus.UNAUTHORIZED)
      .body(ErrorApiResponse.of(ResponseCode.USERNAME_OR_PASSWORD_INCORRECT, ex.getMessage()));
  }

  @ExceptionHandler(AccountDisableException.class)
  public ResponseEntity<ErrorApiResponse> handleAccountDisableException(
    AccountDisableException ex
  ) {
    log.error("Account Disable Exception: {} | username: {}", ex.getMessage(), ex.getUsername());
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(ErrorApiResponse.of(ResponseCode.ACCOUNT_DISABLE, ex.getMessage()));
  }

  @ExceptionHandler(DataNotFoundException.class)
  public ResponseEntity<ErrorApiResponse> handleDataNotFoundException(DataNotFoundException ex) {
    log.error("Data Not Found: {}", ex.getId());
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorApiResponse.of(ResponseCode.NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(PhoneNumberExistedException.class)
  public ResponseEntity<ErrorApiResponse> handlePhoneNumberExistedException(PhoneNumberExistedException ex) {
    log.error("Phone Number Existed: {}", ex.getPhoneNumber());
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ErrorApiResponse.of(ResponseCode.CONFLICT, ex.getMessage()));
  }

  @ExceptionHandler(EmailExistedException.class)
  public ResponseEntity<ErrorApiResponse> handleEmailExistedException(EmailExistedException ex) {
    log.error("Email Existed: {}", ex.getEmail());
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ErrorApiResponse.of(ResponseCode.CONFLICT, ex.getMessage()));
  }
}
