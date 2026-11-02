
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
Daily price × requested quantity × rental days. Rental days are the number of
days between delivery (included) and pickup (excluded).

There is no delivery fee or cancellation fee. The initial invoice policy
applies 19% VAT to the net rental charge. VAT is rounded per invoice line to
whole cents using HALF_UP. This initial policy requires tax-adviser
confirmation before production use.

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
- Invoice numbers use `YYYY-######`, with a global, non-resetting PostgreSQL
  sequence for the numeric portion; gaps are permitted.
- An administrator may create one invoice for a booking only while the booking
  is PENDING or CONFIRMED. An ineligible booking or an existing invoice results
  in a conflict.
- Invoice calculation uses the booking's persisted daily-price snapshot and
  creates one rental line. The line quantity is requested container quantity
  multiplied by half-open rental days, and `totalCents` is the net line amount.
- Invoice totals are net line totals, per-line rounded VAT totals, and their
  sum, respectively.
- Invoice lines are stored separately.
- Invoice totals must be reproducible.
- Payment is initially managed manually.
- Allowed administrator transitions are DRAFT to SENT or VOID, and SENT to
  PAID. PAID and VOID are terminal states.
- Invoice email notifications are deferred to FEAT-001 in the planning
  backlog; creating or changing invoice status sends no email in this
  milestone.
- PDF generation is a future enhancement.

## 8. Administrator Management

- Administrators may transition bookings from PENDING to CONFIRMED or
  CANCELLED, from CONFIRMED to DELIVERED or CANCELLED, and from DELIVERED to
  COMPLETED. CANCELLED and COMPLETED are terminal.
- A booking can be confirmed only when it has a valid invoice whose status is
  PAID. Cancelling a booking releases all unreleased container assignments.
- Administrators may assign or release containers only while a booking is
  PENDING or CONFIRMED. These operations do not change the booking status.
- An assignment requires an AVAILABLE container of the booking's container
  type and must preserve the no-overlap invariant. Assignments may exceed the
  requested booking quantity; over-assignment is logged and the current
  unreleased assignment count is visible on the Booking API response.
- A container's type may be changed only when it has no unreleased assignment
  for a non-cancelled booking whose pickup date is in the future. Changing a
  container to MAINTENANCE or RETIRED is subject to the same restriction.
- A container type may be deactivated or have its details changed only when no
  existing booking references it. Existing booking price snapshots are never
  changed.
- Administrator booking status changes do not send notifications.

## 9. Notifications

- System notifications are limited to customer-initiated booking actions.
- Administrator-initiated status changes do not send notifications.
