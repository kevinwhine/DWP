package uk.gov.dwp.engineering.recruitment.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class RestExceptionHandlerTest {

    @Test
    public void handleInvalidBookingException_Returns400AndCopiesMessage() {

        // Setup
        InvalidBookingException invalidBookingExceptionMock = mock(InvalidBookingException.class);
        doReturn("Invalid booking").when(invalidBookingExceptionMock).getMessage();

        // Act
        final RestExceptionHandler handler = new RestExceptionHandler();
        final ProblemDetail actual = handler.handleInvalidBookingException(invalidBookingExceptionMock);

        // Validate
        assertEquals(actual.getStatus(), HttpStatus.BAD_REQUEST.value());
        assertEquals("Invalid booking", actual.getDetail());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Should extract custom validation message from BindingResult and return HTTP 400 ProblemDetail")
    public void handleMethodArgumentNotValid_withBindingResultErrors_ExtractsMessageAndReturns400() {

        // Arrange: Mock the Spring validation exception framework structures
        MethodArgumentNotValidException mockException = mock(MethodArgumentNotValidException.class);
        BindingResult mockBindingResult =  mock(BindingResult.class);

        // Define a fake FieldError matching your DTO annotation string overrides
        String customErrorMessage = "The account id received is invalid";
        FieldError dummyFieldError = new FieldError("bookingDto", "accountId", customErrorMessage);

        // Stub out the nested stream dependencies
        doReturn(mockBindingResult).when(mockException).getBindingResult();
        doReturn(java.util.List.of(dummyFieldError)).when(mockBindingResult).getFieldErrors();

        // Act: Execute the overridden handler using null placeholders for unused infrastructure inputs
        final RestExceptionHandler handler = new RestExceptionHandler();
        ResponseEntity<Object> responseEntity = handler.handleMethodArgumentNotValid(
                mockException,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                mock(org.springframework.web.context.request.WebRequest.class)
        );

        // Assert: Extract the inner payload body and verify structural integrity
        ProblemDetail actualPayload =
                assertInstanceOf(
                        ProblemDetail.class,
                        responseEntity.getBody(),
                        "Response payload must be a ProblemDetail");

        assertEquals(HttpStatus.BAD_REQUEST.value(), actualPayload.getStatus());
        assertEquals(customErrorMessage, actualPayload.getDetail());
    }
}
