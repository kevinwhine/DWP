package uk.gov.dwp.engineering.recruitment.validation;

import java.math.BigDecimal;

public enum TicketPricing {
    INFANT(0),
    CHILD(17.50),
    ADULT(25.99);

    public final BigDecimal price;

    TicketPricing(final double price) {

        this.price = BigDecimal.valueOf(price);
    }
}
