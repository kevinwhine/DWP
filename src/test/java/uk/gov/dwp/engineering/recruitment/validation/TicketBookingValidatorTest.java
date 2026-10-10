package uk.gov.dwp.engineering.recruitment.validation;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TicketBookingValidatorTest {

    @Test
    public void validateBookingRequest_withAnInvalidAccountId_ThrowsInvalidBookingException() {

        // Setup
        final Long accountId = 0L;

        // Act and Assert
        assertThrows(InvalidBookingException.class, () -> TicketBookingValidator.validateAccountIdAndTicketRequests(accountId));
    }


    @Test
    public void validateBookingRequest_withNULLTicketRequests_ThrowsInvalidBookingException() {

        // Setup
        final Long accountId = 1L;
        final TicketRequest[] ticketRequests = null;

        // Act and Assert
        assertThrows(InvalidBookingException.class, () -> TicketBookingValidator.validateAccountIdAndTicketRequests(accountId, ticketRequests));
    }


    @Test
    public void validateBookingRequest_withTooFewTicketRequests_ThrowsInvalidBookingException() {

        // Setup
        final Long accountId = 1L;
        final TicketRequest[] ticketRequests = new TicketRequest[] {};

        // Act and Assert
        assertThrows(InvalidBookingException.class, () -> TicketBookingValidator.validateAccountIdAndTicketRequests(accountId, ticketRequests));
    }


    @Test
    public void validateBookingRequest_withTooManyTicketRequests_ThrowsInvalidBookingException() {

        // Setup: Dynamically generate 26 (25 + 1) ticket requests
        final Long accountId = 1L;
        TicketRequest[] twentySixRequests = IntStream.range(0, TicketRequestSummary.MAX_TICKETS_TO_REQUEST + 1)
                .mapToObj(i -> new TicketRequest(TicketType.ADULT, 1))
                .toArray(TicketRequest[]::new);

        // Act and Assert
        assertThrows(InvalidBookingException.class, () -> TicketBookingValidator.validateAccountIdAndTicketRequests(accountId, twentySixRequests));
    }

    @Test
    public void validateBookingRequest_withTicketRequestsWithMaxTicketCount_IsAccepted() {

        // Setup: Dynamically generate 25 ticket requests
        final Long accountId = 1L;
        TicketRequest[] twentyFiveRequests = IntStream.range(0, TicketRequestSummary.MAX_TICKETS_TO_REQUEST)
                .mapToObj(i -> new TicketRequest(TicketType.ADULT, 1))
                .toArray(TicketRequest[]::new);

        // Act and Assert
        assertDoesNotThrow(() -> TicketBookingValidator.validateAccountIdAndTicketRequests(accountId, twentyFiveRequests));
    }

    @Test
    public void validateBookingRequest_withAHighTicketCount_throwsInvalidBookingException() {

        //Setup
        final Long accountId = 1L;
        final TicketRequest[] ticketRequestsWithLargeCounts = new TicketRequest[] {
                new TicketRequest(TicketType.ADULT, Integer.MAX_VALUE),
                new TicketRequest(TicketType.CHILD, Integer.MAX_VALUE)
        };

        // Act and Assert
        assertThrows(InvalidBookingException.class, () -> TicketBookingValidator.validateAccountIdAndTicketRequests(accountId, ticketRequestsWithLargeCounts));
    }
}
