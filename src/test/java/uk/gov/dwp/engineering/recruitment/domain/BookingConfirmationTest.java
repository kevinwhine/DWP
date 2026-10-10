package uk.gov.dwp.engineering.recruitment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingConfirmationTest {

  @Test
  void givenBookingConfirmation_whenGetAccount_thenExpectedValueReturned() {
    final BookingConfirmation bookingConfirmation = new BookingConfirmation(99L);
    assertEquals(99L, bookingConfirmation.accountId());
  }


  @Test
  @DisplayName("Should successfully apply fallback defaults when using legacy single-argument constructor")
  void givenBookingConfirmation_whenGetLegacyAccount_thenAccountIdIsReturnedWithDefaults() {
    // Arrange
    Long expectedAccountId = 98765L;

    // Act
    BookingConfirmation confirmation = new BookingConfirmation(expectedAccountId);

    // Assert
    assertEquals(expectedAccountId, confirmation.accountId(), "Account ID should match the input");
    assertEquals(BigDecimal.ZERO, confirmation.totalCost(), "Total cost should default to ZERO");
    assertEquals(0, confirmation.seatCount(), "Total seats should default to 0");
  }
}
