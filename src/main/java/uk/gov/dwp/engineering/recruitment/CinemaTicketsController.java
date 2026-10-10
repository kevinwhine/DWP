package uk.gov.dwp.engineering.recruitment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.dto.BookingDto;
import uk.gov.dwp.engineering.recruitment.dto.TicketRequestDto;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

import java.util.Arrays;

@RestController
@RequestMapping("/cinema")
public class CinemaTicketsController {

  /**
   * The Ticket Booking Service.
   */
  private final CinemaTicketsService cinemaTicketsService;

  public CinemaTicketsController(final CinemaTicketsService cinemaTicketsService) {
    this.cinemaTicketsService = cinemaTicketsService;
  }

  /**
   * Make booking endpoint handler.
   *
   * @return ResponseEntity containing the result of the booking.
   */
  @PostMapping(value = "/bookings", consumes = "application/json", produces = "application/json")
  public ResponseEntity<BookingConfirmation> makeBooking(@Valid @RequestBody BookingDto ticketBookingDto) {

      // Guard against a malformed POST body no deserialising into an instance of Booking
      if (ticketBookingDto == null) {
        throw new InvalidBookingException("No booking details found on request");
      }

      // Translate the requested tickets into the domain model for processing.
      // This is a simple mapping of the DTO to the domain model to accommodate the mismatch in cinema-ticketing.yaml domain models.
      TicketRequest[] ticketRequests = convertTicketRequestDtosToDomain(ticketBookingDto);

      final BookingConfirmation bookingConfirmation = cinemaTicketsService.purchaseTickets(ticketBookingDto.accountId(), ticketRequests);

      /*
       * NOTE FOR ASSESSMENT
       * In a production environment returning an HTTP 201 Created, a 'Location' header
       * pointing to the new resource URI (e.g., via ServletUriComponentsBuilder) should be provided.
       *
       * Since the provided third-party requirements/domain models do not generate or track
       * a unique Booking Identifier, providing a fake ID would violate domain integrity.
       * Therefore, the body is returned directly with a 201 status.
       *
       *     URI location = ServletUriComponentsBuilder
       *                         .fromCurrentRequest()
       *                         .path("/{id}")
       *                         .buildAndExpand(confirmation.bookingId())
       *                         .toUri();
       *     return ResponseEntity.created(location).body(confirmation);
       */
      return new ResponseEntity<>(bookingConfirmation, HttpStatus.CREATED);
  }

    private TicketRequest[] convertTicketRequestDtosToDomain(BookingDto ticketBookingDto) {

        final TicketRequestDto[] ticketRequestDtos = ticketBookingDto.tickets();
        if (ticketRequestDtos != null) {
            return Arrays.stream(ticketRequestDtos)
                    .map(dto -> {
                        // Handle the circumstance when TicketRequestDto[] { ..., null, ... } is passed in the request body.
                        // This is a malformed request and should be rejected with a 400 BAD_REQUEST.
                        if (dto == null) {
                            throw new InvalidBookingException("Ticket request cannot be null");
                        }
                        return dto.toDomain();
                    })
                    .toArray(TicketRequest[]::new);
        }

        return null;
    }

}
