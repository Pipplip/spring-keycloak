
# Business Rules

## 1. Customer

- Every customer is identified by Keycloak subject (sub).
- Customers can only access their own bookings.
- Only one customer type with one billing requirement.

## 2. Container

- Every container belongs to one container type.
- Only active container types can be booked.
- Containers in maintenance cannot be assigned.
- A physical container cannot be assigned
  to overlapping bookings.

## 3. Booking

Booking statuses:
- PENDING
- CONFIRMED
- DELIVERED
- COMPLETED
- CANCELLED

- Pickup date must be after delivery date.
- Every booking belongs to one customer.
- A booking must have a delivery address.
- A new booking starts in PENDING status and reserves the requested physical
  containers.
- A booking can move to CONFIRMED only after payment.
- Status transitions must be validated.

## 4. Availability

- Availability depends on physical inventory.
- Availability is calculated for a rental period.
- Container allocation must be transactional.
- Concurrent bookings must not cause double allocation.

## 5. Pricing

Base calculation:
Daily price × rental days.

There is no delivery fee or cancellation fee. Applicable VAT remains subject
to the approved tax policy.

Historical prices must remain unchanged
after a booking has been created.

## 6. Cancellation

- Customers may cancel bookings in PENDING or CONFIRMED status.
- Cancellation does not incur a fee and releases the booking's container
  assignments.
- Other booking statuses cannot be cancelled by customers.

## 7. Invoicing

Invoice statuses:
- DRAFT
- SENT
- PAID
- VOID

- Every invoice has a unique invoice number.
- Invoice lines are stored separately.
- Invoice totals must be reproducible.
- Payment is initially managed manually.
- PDF generation is a future enhancement.

## 8. Notifications

Send emails when:
- A booking is confirmed.
- A booking is cancelled.
