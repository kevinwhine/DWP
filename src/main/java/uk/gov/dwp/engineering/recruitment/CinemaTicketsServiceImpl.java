package uk.gov.dwp.engineering.recruitment;

import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import uk.gov.dwp.engineering.recruitment.validation.TicketBookingValidator;
import uk.gov.dwp.engineering.recruitment.validation.TicketPricing;
import uk.gov.dwp.engineering.recruitment.validation.TicketRequestSummary;

import java.math.BigDecimal;

/**
 * Core implementation of the {@link CinemaTicketsService} orchestration pipeline.
 *
 * <p><strong>Testing & Integration Assumptions:</strong></p>
 * <p>
 * Per the recruitment assumptions rule out transport problems, downstream network behaviors and transport faults
 * (such as connection timeouts or partial transaction failures) are treated as non-existent.
 * The unit test suite intentionally skips transport layer error-state verification to focus
 * exclusively on pure functional domain math, transaction tracking verification, and structural validation workflows.
 * </p>
 */
@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private final PaymentService paymentService;

  private final SeatReservationService seatReservationService;


  public CinemaTicketsServiceImpl(PaymentService paymentService,
      SeatReservationService seatReservationService) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
  }


  /**
   * Orchestrates the complete ticket purchasing transaction lifecycle.
   *
   * <p>This method executes a multi-step transaction process:
   * <ol>
   *   <li>Performs strict structural business rule validation on incoming parameters.</li>
   *   <li>Computes fractional-safe financial calculations and seating capacities.</li>
   *   <li>Dispatches external transactions sequentially to third-party providers.</li>
   * </ol>
   * </p>
   *
   * @param accountId      the unique identifier of the purchasing customer account.
   * @param ticketRequests a varargs array containing specific ticket types and quantities.
   * @return a valid {@link BookingConfirmation} if all pipeline transactions complete successfully;
   * @throws InvalidBookingException if structural constraints or customer relationships are violated
   *                                 (e.g., child without adult, too many tickets, negative IDs).
   */
  @Override
  public BookingConfirmation purchaseTickets(final Long accountId, final TicketRequest... ticketRequests)
      throws InvalidBookingException {

      // Validate the inbound DTO object of a Booking and throw a business exception InvalidBookingException if there is
      // a validation failure.  This will abort the method and result in a BAD_REQUEST[400] being returned to the client
      // @see uk.gov.dwp.engineering.recruitment.exception.RestExceptionHandler.
      final TicketRequestSummary ticketSummary = TicketBookingValidator.validateAccountIdAndTicketRequests(accountId, ticketRequests);

      // The TicketRequestSummary is a valid combination of tickets so start to process the payment and seating.
      final BigDecimal amountToCharge = calculateTicketCost(ticketSummary);
      paymentService.debitAccount(accountId, amountToCharge);
      final long seatsNeeded = calculateSeatingNeeded(ticketSummary);
      seatReservationService.reserveSeats(accountId, seatsNeeded);

      return new BookingConfirmation(accountId, amountToCharge, seatsNeeded);
  }

  private BigDecimal calculateTicketCost(final TicketRequestSummary summary) {

    return TicketPricing.ADULT.price.multiply(BigDecimal.valueOf(summary.adultTickets())).add(
            TicketPricing.CHILD.price.multiply(BigDecimal.valueOf(summary.childTickets()))
    );
  }

  private long calculateSeatingNeeded(final TicketRequestSummary summary) {

    return (long) summary.adultTickets() + summary.childTickets();
  }
}
