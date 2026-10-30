
# Data Model

## 1. Customer

Represents a locally managed customer profile.

- id: UUID
- keycloakSubject: String (unique)
- email: String (unique)
- firstName: String
- lastName: String
- companyName: String (optional)
- billingAddress: Address
- createdAt: Instant
- updatedAt: Instant

## 2. ContainerType

- id: UUID
- name: String
- description: String
- volumeM3: Decimal (NUMERIC(8,2), greater than 0)
- dailyPriceCents: Long (BIGINT, non-negative integer minor units)
- currencyCode: String (ISO 4217 alphabetic code)
- active: Boolean

## 3. Container

Represents one physical container.

- id: UUID
- inventoryNumber: String (unique)
- containerTypeId: UUID
- status: AVAILABLE | MAINTENANCE | RETIRED

## 4. Booking

- id: UUID
- customerId: UUID
- containerTypeId: UUID
- deliveryDate: LocalDate
- pickupDate: LocalDate
- quantity: Int (positive)
- deliveryAddress: Address
- status: BookingStatus
- dailyPriceCentsSnapshot: Long (BIGINT, non-negative integer minor units)
- deliveryFeeCentsSnapshot: Long (BIGINT, non-negative integer minor units)
- currencyCode: String (ISO 4217 alphabetic code)
- createdAt: Instant
- updatedAt: Instant
- version: Long

## 5. ContainerAssignment

Represents the assignment of physical containers
to a booking.

- id: UUID
- bookingId: UUID
- containerId: UUID
- assignedAt: Instant
- releasedAt: Instant (optional)

## 6. Invoice

- id: UUID
- bookingId: UUID
- invoiceNumber: String (unique)
- netAmountCents: Long (BIGINT, non-negative integer minor units)
- taxAmountCents: Long (BIGINT, non-negative integer minor units)
- grossAmountCents: Long (BIGINT, non-negative integer minor units)
- currencyCode: String (ISO 4217 alphabetic code)
- status: DRAFT | SENT | PAID | VOID
- createdAt: Instant

## 7. InvoiceLine

- id: UUID
- invoiceId: UUID
- description: String
- quantity: Decimal (NUMERIC(12,3), greater than 0)
- unitPriceCents: Long (BIGINT, non-negative integer minor units)
- taxRate: Decimal (NUMERIC(5,4), fraction from 0 to 1)
- totalCents: Long (BIGINT, non-negative integer minor units)

## 8. Relationships

- Customer 1:N Booking
- ContainerType 1:N Container
- ContainerType 1:N Booking
- Booking 1:N ContainerAssignment
- Container 1:N ContainerAssignment
- Booking 1:0..1 Invoice
- Invoice 1:N InvoiceLine

## 9. Database Rules

- UUID primary keys.
- Foreign key constraints.
- Unique constraints.
- Check constraints.
- Appropriate indexes.
- Optimistic locking where needed.
- No overlapping active assignments
  for the same physical container.
- Booking intervals are half-open: delivery date is included and pickup date
  is excluded. Thus, an assignment ending on a pickup date does not overlap
  another starting on that same date.
- Active assignments (releasedAt is null and booking is not cancelled) are
  protected from overlapping booking intervals by a PostgreSQL trigger.
- Customer email and Keycloak subject are unique. Each booking has at most
  one invoice.
- Monetary values use integer minor units, with the ISO currency code stored
  alongside the amount. The API uses `*Cents` fields for those values.
- Invoice-line quantities use three decimal places; tax rates are fractional
  values in the inclusive range 0 to 1 with four decimal places.
