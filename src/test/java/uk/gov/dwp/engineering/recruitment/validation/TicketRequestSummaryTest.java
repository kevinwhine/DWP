package uk.gov.dwp.engineering.recruitment.validation;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import static org.junit.jupiter.api.Assertions.*;

class TicketRequestSummaryTest {

    @Test
    void validateTotalNumberOfTicketsRequested_withZeroTickets_throwsException() {
        // Arrange
        TicketRequestSummary summary = new TicketRequestSummary(0, 0, 0);

        // Act & Assert
        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                summary::validateTotalNumberOfTicketsRequested
        );
        assertEquals("No tickets requested so not point in authorising a payment or seat allocation", exception.getMessage());
    }

    @Test
    void validateAllInfantsHaveAnAdultLapToSitOn_withTooManyInfants_throwsException() {
        // Arrange: 1 Adult, 2 Infants
        TicketRequestSummary summary = new TicketRequestSummary(1, 0, 2);

        // Act & Assert
        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                summary::validateAllInfantsHaveAnAdultLapToSitOn
        );
        assertEquals("Too many infants[2] to accommodate on adult laps[1]", exception.getMessage());
    }

    @Test
    void validateAllChildrenAreAccompaniedByAnAdult_withNoAdult_throwsException() {
        // Arrange: 0 Adults, 2 Children
        TicketRequestSummary summary = new TicketRequestSummary(0, 2, 0);

        // Act & Assert
        InvalidBookingException exception = assertThrows(InvalidBookingException.class,
                summary::validateAllChildrenAreAccompaniedByAnAdult
        );
        assertEquals("Children[2] cannot be accommodated without an Adult[0]", exception.getMessage());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Should successfully validate all rules for an acceptable, mixed family ticket distribution payload")
    void validateAllRules_withValidMixedFamilyPayload_doesNotThrowExceptions() {
        // Arrange: 2 Adults, 2 Children, 1 Infant = 5 total tickets (Well within bounds, enough laps, accompanied)
        TicketRequestSummary summary = new TicketRequestSummary(2, 2, 1);

        // Act & Assert
        assertDoesNotThrow(summary::validateTotalNumberOfTicketsRequested,
                "Valid numeric ticket counts must be approved by the ceiling boundaries");
        assertDoesNotThrow(summary::validateAllInfantsHaveAnAdultLapToSitOn,
                "Infant count lower than or equal to adult count must be approved");
        assertDoesNotThrow(summary::validateAllChildrenAreAccompaniedByAnAdult,
                "Children accompanied by at least one adult must be approved");
    }
}
