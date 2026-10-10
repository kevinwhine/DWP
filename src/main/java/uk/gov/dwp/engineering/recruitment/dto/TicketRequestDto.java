package uk.gov.dwp.engineering.recruitment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

public record TicketRequestDto(
        @NotNull(message = "Malformed request: ticket type selection cannot be null")
        TicketType type,

        @Min(value = 1, message = "Malformed request: ticket count must be at least one") // 💡 Rejects 0 or negative counts
        int count) {

    // Helper mapper to transform the incoming schema cleanly into the unmodifiable domain object
    public TicketRequest toDomain() {
        return new TicketRequest(this.type, this.count);
    }
}
