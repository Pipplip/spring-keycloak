
# Requirements

## 1. Project Goal

Develop a web application for renting waste containers.

Customers can select container types, check availability,
book containers and retrieve invoices.

Administrators manage customers, inventory, bookings and invoices.

## 2. User Groups

### Customers
- No difference between private and business customers.
- Customers can only access their own bookings and invoices.

### Administrators
- Manage container types.
- Manage physical containers.
- Manage bookings.
- Manage invoices.
- Manage customers.

## 3. Functional Requirements

### FR-001: Customer Management
- Customers authenticate through Keycloak.
- Customer profiles are stored locally.
- Only one type of customer is supported.
- Billing addresses are stored.

### FR-002: Container Types
- Display available container types.
- Display volume and daily price.
- Only active types can be booked.

### FR-003: Container Inventory
- Manage individual physical containers.
- Assign containers to bookings.
- Track container availability and status.

### FR-004: Availability
- Customers can check availability.
- Availability depends on container type,
  rental period and physical inventory.

### FR-005: Booking
- Create bookings.
- View own bookings.
- View booking details.
- Cancel eligible bookings.
- Retrieve associated invoices.

### FR-006: Administration
- View and filter all bookings.
- Change booking status.
- Manage container types.
- Manage physical containers.
- Manage invoices.
- Manage customers.

### FR-007: Invoicing
- Create invoices for bookings.
- Calculate invoice amounts.
- Track invoice status.
- Manually mark invoices as paid.

### FR-008: Notifications
- Send booking confirmation emails.
- Send cancellation emails.

### FR-009: Frontend
- Angular SPA
- Customer and administrator interfaces.
- JWT-based authentication.

## 4. Non-Functional Requirements

- Secure authentication and authorization.
- REST API.
- Maintainable architecture.
- Automated testing.
- Docker-based deployment.
- PostgreSQL persistence.

## 5. Open Questions

- Which payment provider should be integrated?
- What is the cancellation period?
- What are minimum and maximum rental durations?
- How is the delivery fee calculated?
- Which VAT rules apply?
- When should invoices be generated?
- Should invoice PDFs be generated?

Open questions must not be resolved
by the agent without approval.
