# PLAN-005: Customer Features

## Objective

Implement the customer-facing container catalogue, period-based availability,
booking creation and retrieval, and cancellation operations described by
`planning/roadmap.md`, using the existing Kotlin/Spring Boot application,
PostgreSQL persistence, Keycloak authentication, and API-first OpenAPI contract.
Keep booking allocation and ownership safe under concurrent requests, and do
not invent unresolved tax, invoice-timing, or payment-provider policies.

## Status

COMPLETED

## Prerequisites

- PLAN-001 Project Setup, PLAN-002 OpenAPI, PLAN-003 Persistence, and PLAN-004
  Security are COMPLETED.
- Existing domain models, PostgreSQL schema, generated OpenAPI interfaces, and
  Keycloak ownership boundary are available.
- The customer routes already exist in `openapi/api.yaml`, but their delegates
  and application workflows are not implemented.
- Customer-feature decisions recorded in this plan have been approved and are
  reflected in the requirements, business rules, data model, and API contract.
  Tax policy, invoice-generation timing, and payment-provider selection remain
  out-of-scope decisions.

## Scope

- Implement public container-type catalogue and detail reads, returning active
  types only and honoring the existing zero-based pagination contract.
- Implement public availability checks by type, requested quantity, and
  half-open rental interval `[deliveryDate, pickupDate)`, considering only
  physical containers with `AVAILABLE` status and existing active assignments.
- Implement authenticated customer booking creation, paginated listing, detail
  retrieval, and the customer invoice retrieval route when an invoice exists.
  Resolve the current customer exclusively through the verified Keycloak `sub`
  and existing `CurrentCustomerService`.
- Create and persist bookings with the correct authenticated owner, requested
  quantity, delivery address, validated dates, daily-price snapshot, and
  PENDING status; reserve inventory transactionally and safely under
  concurrency. A booking can become CONFIRMED only after payment, which is
  outside this milestone.
- Allow customers to cancel PENDING and CONFIRMED bookings without a fee, and
  release their inventory assignments.
- Return API errors consistent with the generated contract and the existing
  RFC 7807 Problem Details handling, including authentication, ownership,
  validation, not-found, and conflict cases.
- Add unit, PostgreSQL integration, API/security, and contract-generation
  coverage. Use existing Docker Compose services for optional local smoke
  testing; do not introduce a frontend application or a backend Compose service
  as part of this milestone.

## Technical Approach

- Preserve API-first development. Implement generated delegate interfaces from
  `build/generated/openapi/kotlin-spring`; do not edit generated sources. The
  current contract includes public catalogue/availability routes and
  authenticated USER booking routes. Change `openapi/api.yaml` first only if an
  approved requirement exposes a contract mismatch, then regenerate Kotlin and
  Angular outputs.
- Keep controllers/delegates thin. Add application use cases/services and
  repository ports in the existing `application` boundary, with Spring MVC,
  Spring Data, and persistence queries/adapters kept in `api` and
  `infrastructure.persistence` as appropriate. Reuse existing domain entities,
  `CustomerRepository`, `CurrentCustomerService`, and
  `BookingOwnershipAuthorizer`; do not couple domain code to Spring Security or
  generated API DTOs.
- Add the required repository queries for active container-type paging,
  availability counts/candidates, customer-scoped booking pages, assignment
  allocation, and invoice lookup by booking. Use database-backed paging rather
  than loading all records into memory.
- Treat availability as an advisory read. On booking creation, revalidate and
  allocate the required physical inventory within one transaction. Preserve
  the database trigger's half-open overlap protection and container-row
  locking; handle allocation conflicts as the documented 409 rather than
  reporting a successful booking without all requested assignments.
- Derive the customer ID from the authenticated profile; never accept or use a
  request-supplied owner ID. Apply ownership checks to booking details,
  cancellation, and invoice retrieval before exposing customer-owned data.
- Validate request constraints at the API boundary and business invariants in
  the application/domain layer. In particular, pickup must follow delivery,
  quantity must be positive, the container type must exist and be active, and
  requested inventory must be available. No minimum or maximum rental duration
  or delivery/cancellation fee applies.
- Reuse the current Gradle Kotlin DSL, JDK 25 toolchain, Spring Boot 3.5.16,
  Kotlin 2.3.21, Flyway, Spring Data JPA, and PostgreSQL 17 Testcontainers.
  Add MockK as a test-only dependency for focused application unit tests; no new
  runtime dependency is required.
- Use Testcontainers for ordinary PostgreSQL behavior tests. Existing Compose
  provides PostgreSQL and Keycloak but not the backend service; optional
  Compose-backed tests must follow the existing `COMPOSE_DATABASE_TEST` opt-in
  convention and must not run application migrations against Keycloak's
  separate database. No Docker Compose change is expected.

## Implementation Steps

1. [x] **Approve feature decisions and scope**
   - [x] Remove delivery and cancellation fees; update the API and persistence
     model accordingly.
   - [x] Reserve inventory on booking creation, set status to PENDING, and
     transition to CONFIRMED only after payment. Payment processing remains
     outside this milestone.
   - [x] Allow cancellation of PENDING and CONFIRMED bookings without a fee;
     release their assignments.
   - [x] Do not impose minimum or maximum rental durations.
   - [x] Include customer invoice retrieval only; defer invoice creation.
   - [x] Return 403 when an authenticated USER has no local customer profile;
     do not auto-provision profiles.
   - These decisions are reflected in the requirements, business rules,
     data model, and API contract before implementation.

2. [x] **Implement public container-type catalogue queries**
   - Add application query services and persistence port methods for listing
     and retrieving active container types.
   - Implement PostgreSQL-backed paging with the contract's `page` and `size`
     limits and map results explicitly to generated API models.
   - Add focused unit and repository/API tests for active/inactive types,
     missing IDs, and pagination metadata.

3. [x] **Implement availability query**
   - Add a domain/application query that rejects invalid intervals and
     non-positive requested quantities and returns not-found for an unknown or
     inactive type as specified by the contract.
   - Calculate available physical inventory for the half-open interval using
     `AVAILABLE` containers and active, unreleased, non-cancelled assignments.
     Ensure adjacent intervals sharing only a boundary date are both available.
   - Add PostgreSQL integration tests for overlapping and touching intervals,
     maintenance/retired containers, assignment release, cancelled bookings,
     quantity thresholds, and availability response fields.

4. [x] **Implement customer booking creation and allocation**
   - Add an application command/use case that gets the owner from
     `CurrentCustomerService`, validates the active type and request, selects
     and allocates the approved quantity, snapshots approved pricing, and
     persists the booking plus assignments in one transaction.
   - Recheck inventory during the transaction and rely on PostgreSQL's existing
     overlap trigger/row-lock strategy to prevent concurrent double
     allocation. Translate allocation races and business conflicts to the
     contract's 409 Problem Detail response.
   - Add unit tests for business validation and PostgreSQL integration tests for
     atomic persistence, exact requested quantity, overlapping concurrent
     requests, and rollback when allocation or persistence fails.

5. [x] **Implement customer booking reads**
   - Add customer-scoped paginated booking retrieval and detail retrieval using
     the trusted authenticated customer identity.
   - Return a customer's own bookings only; do not accept a customer ID from
     query parameters or request bodies. Apply the existing ownership boundary
     to detail reads and map booking snapshots/status/version to the API model.
   - Add tests proving pagination and owner isolation, including missing local
     profiles, unknown bookings, and cross-customer access.

6. [x] **Implement cancellation and invoice retrieval**
   - Implement cancellation only after the approved eligibility and transition
     rules are documented. Apply ownership checks, persist the transition
     transactionally, and release assignments if that behavior is approved.
   - Implement read-only retrieval of the invoice associated with an owned
     booking, returning 404 when no invoice exists. Do not create invoices,
     calculate tax, or determine invoice timing in this milestone.
   - Add tests for cancellation state transitions and ownership, assignment
     release/availability effects, invoice ownership, and absent invoices.

7. [x] **Wire generated API delegates and error responses**
   - Implement the generated catalogue, availability, and booking delegate
     methods, keeping request/response mapping explicit and delegates free of
     business logic.
   - Confirm validation, not-found, conflict, 401, and 403 responses match
     OpenAPI and existing Problem Detail/security handling.
   - Regenerate and validate OpenAPI outputs if the contract was changed by an
     approved decision; never modify generated output manually.

8. [x] **Verify and document the milestone**
   - Run targeted Gradle tests and the backend build with the configured JDK 25
     toolchain; run OpenAPI validation/generation and Angular output validation.
   - Optionally run the existing Compose database smoke test with
     `COMPOSE_DATABASE_TEST=true`; verify the application datasource remains
     separate from Keycloak's database.
   - Update applicable business/API documentation, this plan's status, and
     customer-feature roadmap checkboxes only after approved implementation and
     verification succeed.

## Files and Components Affected

- `openapi/api.yaml` — only if an approved decision requires a contract change;
  it remains the source of truth.
- `backend/build.gradle.kts` — MockK test dependency; run existing
  `openApiValidate`, `generateKotlinOpenApi`, `generateAngularOpenApi`,
  `validateAngularOpenApiOutput`, `test`, and `build` tasks as needed.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/api/**` — new
  generated-delegate implementations and explicit API/domain mappings, using
  the established package layout.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/**` —
  customer feature use cases, query services, and any required output ports.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/domain/**` —
  business validation or behavior only where required by approved rules.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/infrastructure/persistence/**`
  — Spring Data queries, adapters, and transactional allocation support.
- `backend/src/test/kotlin/de/pipplip/abfallcontainerservice/**` — unit,
  repository, PostgreSQL integration, API delegate, security, and availability
  concurrency tests.
- `docs/requirements.md`, `docs/business-rules.md`, `docs/data-model.md`, and
  possibly `docs/architecture.md` — approved requirement or architecture
  clarifications only.
- `README.md` — customer feature test/setup instructions if needed.
- `planning/roadmap.md` and this file — status updates after successful
  implementation only.
- `compose.yaml` — no change expected; existing services support local
  PostgreSQL/Keycloak development and optional database smoke tests.
- Generated sources under `backend/build/generated` and
  `frontend/build/generated` are build output and must not be edited manually.

## Required Database Migrations

Add a new Flyway migration that drops the `bookings.delivery_fee_cents` column.
Do not modify the already-applied V1 migration. The existing PostgreSQL
assignment-overlap trigger and half-open date ranges continue to protect
allocation. Do not create or alter any Keycloak database objects.

## Required API Changes

Update `openapi/api.yaml` before implementing the existing roadmap-listed
customer features. Remove the delivery-fee field from the Booking response and
document PENDING creation, payment-gated confirmation, and cancellation of
PENDING/CONFIRMED bookings without a fee. The contract already defines:

- Public paginated `GET /api/v1/container-types`, active type detail, and
  `GET /api/v1/availability`.
- USER-authenticated `POST /api/v1/bookings`, paginated
  `GET /api/v1/bookings`, booking detail, cancellation, and associated invoice
  retrieval.
- Request/response models, status codes, security requirements, page metadata,
  and Problem Detail responses.

Any approved contract change must be made in `openapi/api.yaml` first and
regenerated through Gradle. Do not add uncontracted fields or endpoints, and do
not hand-edit generated Kotlin or Angular clients.

## Security Implications

- Permit anonymous access only to the documented catalogue and availability
  routes; booking operations require USER authentication under the existing
  Security plan.
- Resolve booking ownership through the verified JWT subject and local
  customer profile. Do not trust customer IDs supplied by clients.
- Scope booking pages to the current customer and check ownership before
  returning booking details, cancelling bookings, or retrieving invoices.
- Do not expose invoices or booking data for another customer. Preserve the
  API's existing 401/403/404 semantics and RFC 7807 responses.
- Use transactional allocation and PostgreSQL overlap protection so concurrent
  booking requests cannot allocate the same physical container for overlapping
  periods.
- Keep database and Keycloak credentials environment-driven. No tokens,
  credentials, or production secrets may be added to source, tests, or
  documentation.

## Testing Requirements

- Unit-test business validation and query/command orchestration using JUnit 5
  and MockK, including date ordering, positive quantities, active type
  requirements, and approved cancellation transitions.
- Use PostgreSQL 17 Testcontainers for repository paging, availability
  calculations, assignment allocation, half-open date semantics, transaction
  rollback, and concurrent allocation conflicts. Do not use an in-memory
  database to validate PostgreSQL-specific behavior.
- Test API delegate/controller behavior with Spring test support, covering
  catalogue pagination, availability, booking creation/list/detail,
  cancellation, invoice retrieval, DTO mapping, and RFC 7807 errors.
- Test anonymous/public versus USER/protected access using the existing
  Security integration pattern. Verify missing customer profiles and
  cross-customer booking/invoice access cannot leak protected data.
- Verify invalid and boundary cases: inactive/unknown container type, invalid
  date range, quantity zero, insufficient inventory, released assignments,
  cancelled bookings, and touching versus overlapping intervals.
- Verify a booking is not partially persisted and no allocation is double
  claimed when competing requests contend for the same inventory.
- Run `.\gradlew.bat test build openApiValidate generateKotlinOpenApi
  generateAngularOpenApi validateAngularOpenApiOutput` from `backend` (or
  equivalent `./gradlew` commands under Bash). Keep Compose database testing
  opt-in with `COMPOSE_DATABASE_TEST=true`.

## Acceptance Criteria

- Public catalogue routes return only active container types and follow the
  OpenAPI paging and response shape.
- Availability reflects eligible physical inventory for the requested
  quantity and half-open rental interval, without counting maintenance,
  retired, released, cancelled, or overlapping allocations as available.
- A booking can be created only for an active type, valid request, and
  sufficient inventory; the authenticated customer is always the owner.
- Booking creation and all required physical assignments are atomic and safe
  against concurrent double allocation.
- Customers can list and retrieve only their own bookings, and customer
  cancellation follows only the approved eligibility and transition policy.
- Customers can retrieve only invoices attached to their own bookings; this
  milestone does not create invoices or define invoice timing.
- API behavior conforms to the existing OpenAPI contract and uses explicit
  validation, Problem Detail, not-found, and conflict responses.
- Existing Flyway migrations remain unchanged; the new migration removes the
  delivery-fee field. No Keycloak database objects, manual generated-source
  edits, customer ID trust, delivery fees, or cancellation fees are introduced.
- Relevant tests, OpenAPI validation/generation checks, and Gradle build pass.
- Applicable docs and roadmap status reflect only approved, verified behavior.

## Definition of Done

- This APPROVED plan and its decisions are reflected in the API and project
  documentation.
- All in-scope features and acceptance criteria are implemented without
  silently resolving open questions.
- Gradle build, relevant unit tests, PostgreSQL integration tests, API/security
  tests, and OpenAPI validation/generation checks pass.
- PostgreSQL transactions and overlap constraints prevent partial or duplicate
  inventory allocation.
- API, security, persistence, and documentation boundaries remain consistent;
  no generated sources are manually modified and no applied migration is
  changed.
- `planning/roadmap.md` is updated only after implementation and verification
  succeed, and this plan is marked COMPLETED at that point.

## Out of Scope

- Administrator operations, inventory/container administration, or container
  type creation and updates; these belong to the Admin Features milestone.
- Invoice creation, amount/tax calculation, invoice state management, payment
  provider integration, PDF generation, and email notifications.
- Angular UI implementation; the repository currently generates and validates
  an Angular client but does not contain an Angular application.
- New Docker Compose services, application Dockerfile, production deployment,
  CI/CD, monitoring, or changes to Keycloak realm/client configuration.
- Changes to completed setup, OpenAPI, persistence, or security milestones
  unless required by an approved customer-feature decision.
- Tax policy, invoice generation timing, payment-provider integration, and
  invoice PDF generation.

## Open Questions

- No blocking customer-feature decisions remain. Payment-provider selection,
  tax rules, invoice generation timing, and PDF generation are outside this
  milestone and remain open for their respective future work.
