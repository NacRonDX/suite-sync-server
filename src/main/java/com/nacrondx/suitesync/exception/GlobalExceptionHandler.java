package com.nacrondx.suitesync.exception;

import com.nacrondx.suitesync.model.user.ErrorResponse;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
      ResourceNotFoundException ex, WebRequest request) {
    var errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(OffsetDateTime.now());
    errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
    errorResponse.setError(HttpStatus.NOT_FOUND.getReasonPhrase());
    errorResponse.setMessage(ex.getMessage());
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
    log.debug(ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {
    var errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(OffsetDateTime.now());
    log.debug(ex.getMessage(), ex);

    if (ex.getMessage() != null && ex.getMessage().contains("already exists")) {
      errorResponse.setStatus(HttpStatus.CONFLICT.value());
      errorResponse.setError(HttpStatus.CONFLICT.getReasonPhrase());
      errorResponse.setMessage(ex.getMessage());
      errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
      return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
    errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
    errorResponse.setMessage(ex.getMessage());
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(
      IllegalStateException ex, WebRequest request) {
    var errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(OffsetDateTime.now());
    errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
    errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
    errorResponse.setMessage(ex.getMessage());
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
    log.debug(ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentialsException(
      BadCredentialsException ex, WebRequest request) {
    var errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(OffsetDateTime.now());
    errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
    errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
    errorResponse.setMessage(ex.getMessage());
    errorResponse.setPath(request.getDescription(false).replace("uri=", ""));
    log.debug(ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
    var path = request.getDescription(false).replace("uri=", "");

    if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
      throw new RuntimeException(ex);
    }

    var errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(OffsetDateTime.now());
    errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
    errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    errorResponse.setMessage("An unexpected error occurred");
    errorResponse.setPath(path);
    log.debug(ex.getMessage(), ex);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
