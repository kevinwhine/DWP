package uk.gov.dwp.engineering.recruitment.domain;

import java.math.BigDecimal;

/**
 * BookingConfirmation is a record that represents the confirmation of a booking.
 * It contains the account ID, total cost of the booking, and total number of seats booked.
 * See cinema-tickets.yaml for contract details.
 */
public record BookingConfirmation(Long accountId, BigDecimal totalCost, Long seatCount) {

    // Constructor for BookingConfirmation with only accountId, defaulting totalCost to BigDecimal.ZERO and seatCount to 0
    // For backwards compatibility with existing code that may only provide the accountId.
    public BookingConfirmation(Long account) {
        this(account, BigDecimal.ZERO, 0L);
    }
}
