package uk.gov.dwp.engineering.recruitment.exception;

import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * Handle InvalidBookingException and return a ProblemDetail response with HTTP 400 status.
   * @param ex The InvalidBookingException that was thrown.
   * @return THe ProblemDetail response with HTTP 400 status and the exception message.
   */
  @ExceptionHandler(InvalidBookingException.class)
  public ProblemDetail handleInvalidBookingException(final InvalidBookingException ex) {

    // Convert the business failure into a conventional HTTP 400 / BAD_REQUEST response
    final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setDetail(ex.getMessage());
    return problemDetail;
  }

  /**
   * Overrides Spring's built-in handler to intercept automatic Jakarta Bean Validation failures,
   * extracts your custom DTO annotation message string, and wraps it cleanly into a
   * standard ProblemDetail structure.
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
          @NonNull MethodArgumentNotValidException ex,
          @NonNull HttpHeaders headers,
          @NonNull HttpStatusCode status,
          @NonNull WebRequest request) {

    // Extract the custom message string from the first validation failure found
    String errorMessage = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse("Invalid request payload parameters specified");

    // Forward the extracted message directly into your existing RFC 7807 handler logic
    ProblemDetail problemDetail = handleInvalidBookingException(new InvalidBookingException(errorMessage));

    // Return the response entity cleanly to satisfy the overridden method structure
    return new ResponseEntity<>(problemDetail, headers, status);
  }
}
