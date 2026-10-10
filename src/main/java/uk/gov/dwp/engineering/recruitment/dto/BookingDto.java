package uk.gov.dwp.engineering.recruitment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BookingDto(
        @NotNull(message = "The account id received is invalid")
        @Min(value = 1, message = "The account id received is invalid") // 💡 Overrides default text
        Long accountId,

        @NotNull(message = "Ticket request array cannot be null or empty")
        @NotEmpty(message = "Ticket request array cannot be null or empty")
        @Valid
        TicketRequestDto[] tickets
) {
    /**
     * Helper constructor to allow for a single parameter to be passed in, which will create a BookingDto with an empty ticket request array.
     * Used to ensure backwards compatibility
     * @param accountId The account identifier
     */
    public BookingDto(Long accountId) {
        this(accountId, null);
    }
}
