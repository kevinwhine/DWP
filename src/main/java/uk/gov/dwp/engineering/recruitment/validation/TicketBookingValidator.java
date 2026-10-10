package uk.gov.dwp.engineering.recruitment.validation;

import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import java.text.MessageFormat;

public class TicketBookingValidator {

    public static TicketRequestSummary validateAccountIdAndTicketRequests(final Long accountId, final TicketRequest... ticketRequests) throws InvalidBookingException {

        if (accountId == null || accountId < 1) {
            throw new InvalidBookingException(MessageFormat.format("The account id[{0}] received is invalid", accountId));
        }

        if (ticketRequests == null) {
            throw new InvalidBookingException("No ticket requests found");
        }

        return validateTicketRequests(ticketRequests);
    }

    private static TicketRequestSummary validateTicketRequests(final TicketRequest... ticketRequests) throws InvalidBookingException {

        if (ticketRequests.length < 1) {
            throw new InvalidBookingException("Too few ticket requests found");
        }

        return validateTotalNumberOfTicketsRequested(ticketRequests);
    }

    private static TicketRequestSummary validateTotalNumberOfTicketsRequested(TicketRequest[] ticketRequests) throws InvalidBookingException {

        final TicketRequestSummary summary = calculateNumberOfTicketsRequestedOfEachType(ticketRequests);

        summary.validateTotalNumberOfTicketsRequested();
        summary.validateAllChildrenAreAccompaniedByAnAdult();
        summary.validateAllInfantsHaveAnAdultLapToSitOn();

        return summary;
    }


    /**
     * Calculates the total number of requested tickets by type.
     *
     * <p>Returns a summary containing the total number of ADULT, CHILD and INFANT
     * tickets across all supplied ticket requests.</p>
     *
     * <p>Assumption: INFANT tickets contribute towards the maximum ticket purchase
     * limit. Although infants are not allocated a seat and do not incur a charge,
     * they are considered a ticket type according to the business rules and are
     * therefore included in the overall ticket count.</p>
     *
     * NB. Perform validations on the ticket requests to ensure that they are not null, have
     * a valid ticket type, and have a valid ticket count. If any of these validations fail,
     * an InvalidBookingException is thrown.
     *
     * @param ticketRequests the requested ticket quantities by ticket type
     * @return a summary of the total adult, child and infant tickets requested
     */
    private static TicketRequestSummary calculateNumberOfTicketsRequestedOfEachType(final TicketRequest[] ticketRequests) throws InvalidBookingException{

        long adult = 0;
        long child = 0;
        long infant = 0;

        // Keep an extra running total of the number of tickets requested to ensure that we do not exceed the maximum allowed.
        // To avoid integer overflow, we use a long for the running total.  We will cast back to int when creating the TicketRequestSummary
        long totalTickets = 0;

        for (final TicketRequest ticketRequest : ticketRequests) {
            if (ticketRequest == null) {
                throw new InvalidBookingException("Invalid null ticket request found");
            }
            if (ticketRequest.type() == null) {
                throw new InvalidBookingException("Invalid and unknown ticket type found");
            }

            int count = getCount(ticketRequest, totalTickets);

            // The count of ticket requests is valid, so we can safely add it to the running total of tickets requested.
            totalTickets += count;

            // Now we can safely add the count to the appropriate ticket type total.
            switch (ticketRequest.type()) {
                case ADULT -> adult += count;
                case CHILD -> child += count;
                case INFANT -> infant += count;
            }
        }

        // Now safe to cast back to int as we have validated that the total number of tickets requested does not exceed
        // the maximum allowed.
        return new TicketRequestSummary((int) adult, (int) child, (int) infant);
    }


    private static int getCount(final TicketRequest ticketRequest, final long totalTickets) {

        int count = ticketRequest.ticketCount();
        if (count < 1) {
            throw new InvalidBookingException(
                    MessageFormat.format("Invalid number of tickets requested[{0}]", count));
        }
        if (count > TicketRequestSummary.MAX_TICKETS_TO_REQUEST - totalTickets) {
            throw new InvalidBookingException(
                    MessageFormat.format("More tickets requested than allowed[{0}]",
                            TicketRequestSummary.MAX_TICKETS_TO_REQUEST));
        }

        return count;
    }
}
