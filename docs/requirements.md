
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
- Cancel bookings in PENDING or CONFIRMED status without a cancellation fee.
- Retrieve associated invoices.
- Newly created bookings are PENDING and reserve the requested inventory.
- A booking becomes CONFIRMED only after payment; payment processing is not
  implemented in the Customer Features milestone.

### FR-006: Administration
- View and filter all bookings.
- Change booking status.
- Manage container types.
- Manage physical containers.
- Manage invoices.
- Manage customers.
- Administrators may manually assign more containers than the quantity
  requested by a booking; the system records this event and makes the active
  assignment count visible on the booking response.

### FR-007: Invoicing
- Create invoices for bookings.
- Calculate invoice amounts.
- Track invoice status.
- Manually mark invoices as paid.

### FR-008: Notifications
- Send booking confirmation emails.
- Send cancellation emails.

### FR-009: Frontend
- Provide a responsive Angular single-page application for customer and
  administrator workflows.
- Customers can browse active container types, check availability, create and
  view their own bookings, cancel eligible bookings, and retrieve associated
  invoices.
- Administrators can manage bookings, container types, physical containers,
  customers, and invoices through the existing API.
- Authenticate with the existing Keycloak realm using OpenID Connect
  Authorization Code Flow with PKCE. The browser application is a public
  client and must not contain a client secret.
- Show clear loading, empty, validation, success, and API error states. Render
  API Problem Details as user-safe messages without exposing internal details.
- Provide keyboard-operable controls, semantic markup, visible focus, and
  layouts that work on mobile and desktop.

## 4. Non-Functional Requirements

- Secure authentication and authorization.
- REST API.
- The frontend consumes the API contract and does not access PostgreSQL
  directly.
- The backend remains authoritative for authorization and customer ownership;
  frontend route guards are a usability measure, not a security boundary.
- Target WCAG 2.2 AA for the customer and administrator interfaces.
- Maintainable architecture.
- Automated testing.
- Docker-based deployment.
- PostgreSQL persistence.

## 5. Open Questions

- Which payment provider should be integrated?
- Which VAT rules apply?
- When should invoices be generated?
- Should invoice PDFs be generated?

Open questions must not be resolved
by the agent without approval.
