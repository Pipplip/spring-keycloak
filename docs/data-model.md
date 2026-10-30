
# Data Model

## 1. Customer

Represents a locally managed customer profile.

- id: UUID
- keycloakSubject: String (unique)
- email: String
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
- volume: Decimal
- dailyPrice: Decimal
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
- deliveryAddress: Address
- status: BookingStatus
- dailyPriceSnapshot: Decimal
- deliveryFeeSnapshot: Decimal
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
- netAmount: Decimal
- taxAmount: Decimal
- grossAmount: Decimal
- status: DRAFT | SENT | PAID | VOID
- createdAt: Instant

## 7. InvoiceLine

- id: UUID
- invoiceId: UUID
- description: String
- quantity: Decimal
- unitPrice: Decimal
- taxRate: Decimal
- total: Decimal

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
