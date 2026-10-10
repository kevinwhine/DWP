package uk.gov.dwp.engineering.recruitment.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BookingDtoTest {

    @Test
    public void ctor_withSingleParameter_shouldCreateBookingDto() {

        // Setup
        Long accountId = 123L;

        // Act
        BookingDto bookingDto = new BookingDto(accountId);

        // Validate
        assertEquals(accountId, bookingDto.accountId(), "Account ID should match constructor input");
        assertNull(bookingDto.tickets(), "Tickets array payload must default to null for legacy compliance tracking");
    }
}
