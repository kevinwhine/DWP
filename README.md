# Cinema Tickets API - Implementation Summary

## 🎯 Objective
A Spring Boot implementation of the cinema ticket booking service that meets the supplied business rules, constraints and assumptions.

---

## 📋 Business Rule Interpretation & Ambiguity Resolution

The DWP assessment criteria explicitly evaluate a candidate's ability to identify business rule ambiguities. The following assumptions and interpretations were made where the business rules were ambiguous:

### 👪 Family Structure & Lap Safety
* **The Ambiguity:** The requirements state that *“An INFANT... will be sitting on an ADULT lap”* and *“Infant tickets cannot be purchased without purchasing an ADULT ticket.”* However, it does not define a physical safety ceiling for how many infants an adult can carry.
* **The Resolution:** For physical safety and logical consistency, this engine enforces a **1:1 Infant-to-Adult ratio**. A booking is rejected if the number of infants exceeds the number of adults (e.g., 1 Adult cannot hold 2 Infants on their lap simultaneously).
* **Unaccompanied Minors:** Bookings containing `CHILD` or `INFANT` ticket classes are rejected automatically if the accompanying `ADULT` ticket count equals `0`.

### ⚠️ Ticket Capacity Calculations & Limits
* **The Ambiguity:** The rules state *“Only a maximum of 25 tickets can be purchased at a time.”* It is ambiguous whether this limit means 25 *per ticket class* or 25 *cumulative total units*.
* **The Resolution:** This engine interprets the limit as applying to the total number of tickets in a booking. The combined number of ADULT, CHILD and INFANT tickets cannot exceed 25.
* **Empty and Below-One Requests:** Requests with no tickets, or ticket counts below one, are rejected before any downstream processing.
* **Infant Seating Exclusions:** While infants count toward the 25-ticket bulk transaction limit, they are completely excluded from the seat allocation calculation requested downstream from the `SeatReservationService`.

---

## 🛠️ Architectural & Integration Assumptions

* **Downstream Transport Reliability:** In alignment with the provided code test rules (*"The payment will always go through"* and *"The seats will always be reserved"*), this implementation acts purely as an orchestrator and treats payment and seating operations as atomic and flawless. There are no signatures or requirements for handling `IOException` or `TimeoutException`.
* **Domain Model Modification (`BookingConfirmation`):** Per the assessment constraints, the `uk.gov.dwp.engineering.recruitment.domain` package is protected from modifications with the explicit exception of `BookingConfirmation`. Because Java records are implicitly final and cannot be extended via standard inheritance, this contract was enhanced in a non-breaking manner using constructor overloading. This exposes the calculated transaction cost and reserved seat count directly to the API consumer.

---

## 🛠️ Validation Strategy

Validation is performed at two layers.
The REST API uses Jakarta Bean Validation to reject malformed requests with a HTTP 400 response.
Equivalent validation is retained within the business validation layer. Although this overlaps with the REST validation, it was kept intentionally so that business rules remain self-contained and independently testable.

---

## 🧪 Testing Strategy & Assumptive Scope

### Downstream Infrastructure
In compliance with the provided assessment guidelines, this solution explicitly assumes the external third-party infrastructure is completely defect-free. Consequently, network transport errors, gateway timeouts, and partial fallback scenarios have been intentionally excluded from the unit test suite to remain aligned with the specified bounds of the test.

### Functional Boundary Coverage
Testing focuses on the business rules and invalid purchase scenarios defined in the assessment. Controller tests verify API behaviour, whilst service and validator tests verify business rule enforcement.
* **Account ID Guardrails:** Ensuring IDs less than 1 are systematically rejected.
* **Capacity Constraints:** Enforcing the maximum ceiling of 25 ticket units per request (proving the 25-ticket maximum edge case passes cleanly, while 26-ticket payloads are rejected).
* **Zero & Negative Thresholds:** Explicitly verifying that **completely empty ticket requests** or **individual ticket request quantities below one** seamlessly trigger an HTTP 400 Bad Request.
* **Family Structuring:** Rejecting unaccompanied children or infants lacking adult lap matches.

## 🛠️ Postman Collection
A Postman collection has been provided within the /postman directory to support manual testing of the API.

The collection includes example requests covering both valid and invalid booking scenarios, including:

Valid bookings
Maximum ticket boundary validation
Invalid account IDs
Child and infant booking rule violations
Infant-to-adult ratio validation
Maximum ticket limit violations
Invalid ticket counts
Empty request payloads


Validation is performed at two layers.
The REST API uses Jakarta Bean Validation to reject malformed requests with a HTTP 400 response.
Equivalent validation is retained within the business validation layer. Although this overlaps with the REST validation, it was kept intentionally so that business rules remain self-contained and independently testable.


#### 🔄 API Contract Boundary & Domain Mapping

- **The Issue:** A structural mismatch exists between the published OpenAPI contract and the immutable domain model supplied with the exercise.
- **Examples:** The OpenAPI document exposes `tickets[].count`, whilst the domain model exposes `ticketRequests[].ticketCount`.
- **The Constraint:** The assessment rules prohibit modification of classes within the `uk.gov.dwp.engineering.recruitment.domain` package.
- **The Resolution:** A dedicated DTO layer was introduced at the REST boundary. Incoming request payloads are deserialised into DTOs that align exactly with the published OpenAPI specification and are then mapped into the immutable domain objects before invoking the service layer.
- **Rationale:** This preserves the published API contract, respects the domain package immutability constraint, and maintains a clear separation between transport concerns and business-domain concerns.
