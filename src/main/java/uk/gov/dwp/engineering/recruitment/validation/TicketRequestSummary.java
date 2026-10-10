package uk.gov.dwp.engineering.recruitment.validation;

import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import java.text.MessageFormat;

public record TicketRequestSummary(int adultTickets, int childTickets, int infantTickets) {

    static final int MAX_TICKETS_TO_REQUEST = 25;
    static final int MIN_TICKETS_TO_REQUEST = 1;

    public int totalTickets() {

        return adultTickets() + childTickets() + infantTickets();
    }

    public void validateTotalNumberOfTicketsRequested() throws InvalidBookingException {

        if (totalTickets() < MIN_TICKETS_TO_REQUEST) {
            throw new InvalidBookingException("No tickets requested so not point in authorising a payment or seat allocation");
        }
        // the MAX test is not strictly necessary as the TicketBookingValidator will have already performed this check,
        // but it is included here for completeness.
        if (totalTickets() > MAX_TICKETS_TO_REQUEST) {
            throw new InvalidBookingException("Too many tickets requested. Maximum allowed is " + MAX_TICKETS_TO_REQUEST);
        }
    }

    public void validateAllInfantsHaveAnAdultLapToSitOn() throws InvalidBookingException {

        if (infantTickets() > adultTickets()) {
            throw new InvalidBookingException(MessageFormat.format("Too many infants[{0}] to accommodate on adult laps[{1}]", infantTickets(), adultTickets()));
        }
    }

    public void validateAllChildrenAreAccompaniedByAnAdult() throws InvalidBookingException {

        if (childTickets() > 0 && adultTickets() < 1) {
            throw new InvalidBookingException(MessageFormat.format("Children[{0}] cannot be accommodated without an Adult[{1}]", childTickets(), adultTickets()));
        }
    }
}
