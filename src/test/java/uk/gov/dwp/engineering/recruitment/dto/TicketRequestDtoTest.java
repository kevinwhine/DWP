package uk.gov.dwp.engineering.recruitment.dto;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TicketRequestDtoTest {

    @Test
    public void toDomain_withTypeAndTicketCount_ConvertToDomainObject() {

        TicketRequestDto dto = new TicketRequestDto(TicketType.ADULT, 2);
        TicketRequest domain = dto.toDomain();

        assertEquals(TicketType.ADULT, domain.type());
        assertEquals(2, domain.ticketCount());
    }
}
