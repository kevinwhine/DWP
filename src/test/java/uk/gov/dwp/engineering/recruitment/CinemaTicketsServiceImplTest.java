package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private CinemaTicketsServiceImpl cinemaTicketsService;

    @Test
    public void purchaseTickets_withValidMixedTicketRequests_calculatesCorrectCostAndSeats() throws InvalidBookingException {
        // Arrange
        final Long accountId = 1L;
        final TicketRequest adultRequests = new TicketRequest(TicketType.ADULT, 2);   // 2 * £25.99 = £51.98
        final TicketRequest childRequests = new TicketRequest(TicketType.CHILD, 1);   // 1 * £17.50 = £17.50
        final TicketRequest infantRequests = new TicketRequest(TicketType.INFANT, 1); // 1 * £0.00  = £0.00

        // Exact target allocations to verify
        final BigDecimal expectedCost = new BigDecimal("69.48");
        final Long expectedSeatCount = 3L; // 2 Adults + 1 Child; the infant sits on an adult's lap.

        // Standard stubbing setup matching the "always succeeds" assumption
        when(paymentService.debitAccount(accountId, expectedCost))
                .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));
        when(seatReservationService.reserveSeats(accountId, expectedSeatCount))
                .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // Act
        BookingConfirmation confirmation = cinemaTicketsService.purchaseTickets(accountId, adultRequests, childRequests, infantRequests);

        // Validate
        assertNotNull(confirmation, "Booking confirmation must be returned upon successful execution flow");
        assertEquals(accountId, confirmation.accountId(), "Confirmation must map back to the purchasing customer account ID");
        assertEquals(expectedCost, confirmation.totalCost(), "Confirmation must contain the total safe financial calculation fee accumulated");
        assertEquals(expectedSeatCount, confirmation.seatCount(), "Confirmation must contain the number of seats reserved");

        verify(paymentService, times(1)).debitAccount(accountId, expectedCost);
        verify(seatReservationService, times(1)).reserveSeats(accountId, expectedSeatCount);
    }

    @Test
    public void purchaseTickets_whenValidatorThrowsException_shortCircuitsAndPreventsExternalCalls() {
        // Arrange
        final Long invalidAccountId = -5L; // Invalid ID boundary condition
        final TicketRequest adultRequests = new TicketRequest(TicketType.ADULT, 1);

        // Act & Assert
        assertThrows(InvalidBookingException.class, () -> cinemaTicketsService.purchaseTickets(invalidAccountId, adultRequests));

        // Verifies your code safely protects external APIs from handling garbage input data
        verifyNoInteractions(paymentService, seatReservationService);
    }
}
