package uk.gov.dwp.engineering.recruitment.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvalidBookingExceptionTest {

  @Test
  void givenInvalidBookingException_whenGetMessage_thenExpectedValueReturned() {
    final InvalidBookingException invalidBookingException = new InvalidBookingException(
        "A useful message");
    assertEquals("A useful message", invalidBookingException.getMessage());
  }

}
