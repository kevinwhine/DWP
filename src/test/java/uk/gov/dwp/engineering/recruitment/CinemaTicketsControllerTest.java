package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.dto.BookingDto;
import uk.gov.dwp.engineering.recruitment.dto.TicketRequestDto;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(CinemaTicketsController.class)
@Import(CinemaTicketsServiceImpl.class)
class CinemaTicketsControllerTest {

    final static String QUERY_PATH = "/cinema/bookings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean(reset = MockReset.AFTER)
    private PaymentService paymentService;

    @MockitoBean(reset = MockReset.AFTER)
    private SeatReservationService seatReservationService;

    /**
     * Validates that a null booking payload is intercepted by the controller guard
     * and translated cleanly into an HTTP 400 Bad Request response.
     */
    @Test
    public void makeBooking_withANullBooking_returnsHttp400BadRequest() throws Exception {

        // Setup, Act and Validate
        mockMvc.perform(post(QUERY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("")) // Empty body triggers the null payload protection clause
                .andExpect(status().isBadRequest());
    }

    @Test
    public void makeBooking_withNullAccountId_rejectsBookingBeforeAPICalls() throws Exception {

        assertBookingRejectedWith400(
                new BookingDto(
                        null,
                        new TicketRequestDto[]{
                                new TicketRequestDto(TicketType.ADULT, 1)
                        }),
                null);
    }

    @Test
    public void makeBooking_withInvalidAccountId_rejectsBookingBeforeAPICalls() throws Exception {

        // Arrange: Build a raw JSON string where accountId is explicitly set to an invalid boundary value
        final String invalidAccountIdJson = """
            {
                "accountId": 0,
                "tickets": [
                    {
                        "type": "ADULT",
                        "count": 1
                    }
                ]
            }
            """;

        // Convert the string into a DTO using your autowired objectMapper
        final BookingDto invalidBooking = objectMapper.readValue(invalidAccountIdJson, BookingDto.class);

        // Act and validate: Expect HTTP 400 Bad Request for an invalid accountId
        assertBookingRejectedWith400(invalidBooking,"The account id received is invalid");
    }

    @Test
    public void makeBooking_withEmptyTicketArray_rejectsBookingBeforePaymentOrSeatReservation() throws Exception {

        assertBookingRejectedWith400(
                new BookingDto(
                        1L,
                        new TicketRequestDto[]{}),
                null);
    }


    /**
     * Validates that a structurally clean booking successfully returns an HTTP 201 Created status.
     */
    @Test
    public void makeBooking_WithAValidBooking_ReturnsCreated201() throws Exception {

        // Setup: A valid booking with one adult ticket
        final BookingDto booking = new BookingDto(1L, new TicketRequestDto[] {
                new TicketRequestDto(TicketType.ADULT, 1)
        });

        // Mock the PaymentService and SeatReservationService to return a successful response.
        // Not moving the CinemaTicketsService to a mock because we want to test the controller's behavior with a valid
        // booking, and we need to ensure that the downstream services are called correctly, and to allow the controller
        // to return a 201 Created status.
        final ResponseEntity<String> responseEntitySuccessMock = new ResponseEntity<>("Mock Success", HttpStatus.OK);
        doReturn(responseEntitySuccessMock).when(paymentService).debitAccount(anyLong(), any(BigDecimal.class));
        doReturn(responseEntitySuccessMock).when(seatReservationService).reserveSeats(anyLong(), anyLong());

        // Act and validate: Expect HTTP 201 Created for a valid booking
        mockMvc.perform(post(QUERY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1L))
                .andExpect(jsonPath("$.totalCost").value(25.99))
                .andExpect(jsonPath("$.seatCount").value(1));

        verify(paymentService, times(1)).debitAccount(eq(1L), eq(new BigDecimal("25.99")));
        verify(seatReservationService, times(1)).reserveSeats(eq(1L), eq(1L));
    }

    /**
     * Validates that a booking with invalid ticket requests is rejected before any downstream calls.
     */
    @Test
    public void makeBooking_withInvalidTicketRequests_rejectsBookingBeforePaymentAndSeatAllocationCalls() throws Exception {

        // Case 1: Zero Adults is invalid
        final String zeroAdultsJson = """
            {
                "accountId": 1,
                "tickets": [{ "type": "ADULT", "count": 0 }]
            }
            """;
        final BookingDto zeroAdultsBooking = objectMapper.readValue(zeroAdultsJson, BookingDto.class);
        assertBookingRejectedWith400(zeroAdultsBooking, null);

        // Case 2: A ticket request with a negative number of tickets is invalid.
        final String negativeTicketsJson = """
            {
                "accountId": 1,
                "tickets": [{ "type": "ADULT", "count": -1 }]
            }
            """;
        final BookingDto negativeTicketsBooking = objectMapper.readValue(negativeTicketsJson, BookingDto.class);
        assertBookingRejectedWith400(negativeTicketsBooking, null);

        // Case 3: A booking with a null ticket request array is invalid.
        assertBookingRejectedWith400(new BookingDto(1L, (TicketRequestDto[]) null), null);

        // Case 4: A booking with a null ticket type is invalid.
        assertBookingRejectedWith400(new BookingDto(1L, new TicketRequestDto[] {
                new TicketRequestDto(null, 1)
        }), null);
    }

    /**
     * Validates that a booking with more than 25 tickets is rejected before any payment or seat reservation calls are
     * made
     */
    @Test
    public void makeBooking_withMoreThan25Tickets_rejectsBookingBeforePaymentAndSeatAllocationCalls() throws Exception {

        // One Adult and 26 tickets
        assertBookingRejectedWith400(new BookingDto(1L, new TicketRequestDto[] {
                new TicketRequestDto(TicketType.ADULT, 26)
        }), null);
    }

    /**
     * Validates that a booking with children and no adults is rejected before any payment or seat reservation calls
     * are made to the PaymentService or SeatReservationService.
     */
    @Test
    public void makeBooking_withChildrenAndNoAdult_rejectsBookingBeforePaymentAndSeatAllocationCalls() throws Exception {

        // No Adult tickets is invalid because each Child must have an Adult to accompany them.
        assertBookingRejectedWith400(new BookingDto(1L, new TicketRequestDto[] {
                new TicketRequestDto(TicketType.CHILD, 1)
        }), null);
    }

    /**
     * Asserts that a booking is rejected due to having no tickets requested.
     */
    @Test
    public void makeBooking_withNoTickets_rejectsBookingBeforePaymentOrSeatReservation() throws Exception {

        // Zero ticket requests is invalid because at least one ticket must be requested.
        assertBookingRejectedWith400(new BookingDto(1L), null);
    }

    @Test
    public void makeBooking_atMaximumCombinedBoundary_executesSuccessfully() throws Exception {

        // Arrange: 15 Adults + 5 Children + 5 Infants = 25 Total Tickets (Exactly on the limit ceiling)
        final BookingDto boundaryBookingDto = new BookingDto(
                12345L,
                new TicketRequestDto[] {
                        new TicketRequestDto(TicketType.ADULT, 15),
                        new TicketRequestDto(TicketType.CHILD, 5),
                        new TicketRequestDto(TicketType.INFANT, 5)
                }
        );

        // Loose matchers tell Mockito to accept ANY long and ANY big decimal during execution
        final ResponseEntity<String> responseEntitySuccessMock = new ResponseEntity<>("Mock Success", HttpStatus.OK);
        doReturn(responseEntitySuccessMock).when(paymentService).debitAccount(anyLong(), any(BigDecimal.class));
        doReturn(responseEntitySuccessMock).when(seatReservationService).reserveSeats(anyLong(), anyLong());

        // Act & Assert (The strict verification happens on the HTTP response payload)
        mockMvc.perform(post(QUERY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boundaryBookingDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(12345L))
                .andExpect(jsonPath("$.totalCost").value(477.35))
                .andExpect(jsonPath("$.seatCount").value(20)); // 15 Adults + 5 Children
    }


    @Test
    public void makeBooking_withMatchingAdultAndInfantCounts_executesSuccessfully() throws Exception {

        BookingDto booking = new BookingDto(
                1L,
                new TicketRequestDto[]{
                        new TicketRequestDto(TicketType.ADULT, 2),
                        new TicketRequestDto(TicketType.INFANT, 2)
                });

        ResponseEntity<String> responseEntitySuccessMock =
                new ResponseEntity<>("Mock Success", HttpStatus.OK);

        doReturn(responseEntitySuccessMock)
                .when(paymentService)
                .debitAccount(anyLong(), any(BigDecimal.class));

        doReturn(responseEntitySuccessMock)
                .when(seatReservationService)
                .reserveSeats(anyLong(), anyLong());

        mockMvc.perform(post(QUERY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatCount").value(2));
    }

    @Test
    public void makeBooking_withInfantOverflow_returnsStructuredProblemDetail() throws Exception {
        // Arrange: 1 Adult but 2 Infants (Violates physical safety lap logic)
        final BookingDto invalidBookingDto = new BookingDto(
                99L,
                new TicketRequestDto[] {
                        new TicketRequestDto(TicketType.ADULT, 1),
                        new TicketRequestDto(TicketType.CHILD, 2),
                        new TicketRequestDto(TicketType.INFANT, 2)
                }
        );

        // Act & Assert
        assertBookingRejectedWith400(invalidBookingDto, "Too many infants[2] to accommodate on adult laps[1]");
    }


    /**
     * Validates that an explicit null element embedded inside the tickets array
     * is intercepted safely at the controller stream boundary, returning HTTP 400
     * instead of throwing an unhandled NullPointerException (HTTP 500).
     */
    @Test
    public void makeBooking_withNullElementInTicketsArray_returnsHttp400BadRequestBeforeAPICalls() throws Exception {

        // Setup: Explicit raw JSON string reflecting an element array corruption
        final String malformedJsonPayload = """
            {
                "accountId": 12345,
                "tickets": [
                    null,
                    {
                        "type": "ADULT",
                        "count": 1
                    }
                ]
            }
            """;
        // 💡 Convert the raw JSON string directly into a BookingDto using Jackson
        final BookingDto corruptedBookingDto = objectMapper.readValue(malformedJsonPayload, BookingDto.class);

        // Act & Assert
        assertBookingRejectedWith400(corruptedBookingDto, "Ticket request cannot be null");
//        mockMvc.perform(post(QUERY_PATH)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(malformedJsonPayload))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.detail").value("Ticket request cannot be null"));

        // Verify that the processing failed fast before interacting with any external services
        verifyNoInteractions(paymentService, seatReservationService);
    }

    /**
     * Dry orchestration helper simulating an HTTP integration request to assert error reporting formatting contracts.
     *
     * @param booking The booking that is rejected due to invalid parameters. This method asserts that the booking is
     *                rejected before any downstream calls are made to the PaymentService or SeatReservationService.
     */
    private void assertBookingRejectedWith400(final BookingDto booking, final String expectedErrorMessageSubstring) throws Exception {

        ResultActions resultActions = mockMvc.perform(post(QUERY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(booking)))
                .andExpect(status().isBadRequest());

        // Does the detail message matter in the validation?
        if (expectedErrorMessageSubstring != null) {
            resultActions.andExpect(jsonPath("$.detail").value(expectedErrorMessageSubstring));
        }

        // Validate that the paymentService and seatReservationService were not called due to the invalid booking
        // parameters.
        verifyNoInteractions(paymentService, seatReservationService);
    }
}
