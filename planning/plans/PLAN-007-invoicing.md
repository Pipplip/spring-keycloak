# PLAN-007: Invoicing

## Objective

Implement the Invoicing roadmap milestone on the existing Kotlin, Spring Boot,
PostgreSQL, and Keycloak foundation. Create and calculate booking invoices
and support the documented invoice lifecycle and access patterns. Invoice
email notifications are deferred to FEAT-001 in the planning backlog.

## Status

COMPLETED

## Prerequisites

- PLAN-001 Project Setup, PLAN-002 OpenAPI, PLAN-003 Persistence, PLAN-004
  Security, PLAN-005 Customer Features, and PLAN-006 Admin Features are
  COMPLETED.
- Invoice and invoice-line domain/persistence models and database tables
  already exist. Customer invoice retrieval, admin invoice listing/detail,
  admin invoice creation for a booking, and manual mark-paid operations are
  already represented in `openapi/api.yaml`; customer retrieval is already
  implemented.
- Invoice creation, calculation, numbering, and lifecycle decisions are
  recorded below and in the relevant requirements and design documentation.
- Invoice email notifications are deferred to FEAT-001 by user direction.

## Scope

- Implement administrator invoice creation for an eligible booking, including
  calculation and persistence of invoice lines and reproducible totals under
  the approved rules.
- Implement administrator invoice retrieval/listing and manual payment-status
  management according to the existing API contract and any approved contract
  changes.
- Preserve customer retrieval of an invoice only for the authenticated
  customer's own booking, deriving ownership from the verified Keycloak
  subject.
- Defer invoice email notifications to FEAT-001; do not add provider
  configuration or send emails as part of this milestone.
- Add unit, PostgreSQL integration, and API/security tests; document invoice
  policies and local testing.
- Add Flyway migrations only for approved schema changes. Never modify an
  already-applied migration.

## Technical Approach

- Preserve API-first development with `openapi/api.yaml` as the source of truth.
  The contract already defines customer invoice retrieval, administrator
  invoice list/detail/create, and mark-paid routes. Change the contract only
  for approved missing behavior, then regenerate Kotlin and Angular outputs;
  never edit generated sources manually.
- Keep delegates thin. Put invoice creation, calculation, and lifecycle
  validation in application/domain code; keep invoice persistence, paging, and
  any transactional number allocation in infrastructure adapters. Continue
  separating domain models, generated API models, and JPA entities.
- Reuse the existing invoice records, invoice-line records, unique
  `booking_id`, unique `invoice_number`, integer minor-unit amounts, currency
  code, status enum, line quantity precision, and tax-rate storage. Do not
  change the schema unless an approved decision requires a new migration.
- Calculate from the booking's persisted daily-price snapshot, requested
  quantity, and half-open rental days. Apply the initial 19% VAT rate to net,
  rounding VAT per line using HALF_UP; keep monetary arithmetic exact in
  integer minor units. This initial policy requires tax-adviser confirmation
  before production use.
- Reuse the existing Spring Security/Keycloak configuration and role
  boundaries: administrator operations require ADMIN, and customer retrieval
  requires USER ownership derived from the verified JWT subject. Never accept a
  customer identity supplied by the caller.
- Use the existing Gradle Kotlin DSL, JDK 25 toolchain, Spring Boot 3.5.16,
  Kotlin 2.3.21, PostgreSQL 17, Flyway, Spring Data JPA, MockK, and PostgreSQL
  Testcontainers. No mail dependency is needed for this milestone.
- Keep local database configuration environment-driven. Existing Compose runs
  PostgreSQL 17 and Keycloak with a separate Keycloak database; application
  migrations and tests must continue to target only the application database.
  Do not add email services or credentials. Do not add credentials or tokens
  to source-controlled configuration.
- Keep PDF generation out of scope; the business rules describe it as a future
  enhancement.

## Implementation Steps

1. [x] **Record invoice policies**
   - Record the approved creation timing, eligibility, calculation, VAT,
     numbering, and invoice status-transition decisions below and in the
     relevant project documentation.
   - Email notifications are deferred to FEAT-001; provider, recipient,
     content, and delivery/retry decisions are not needed for this milestone.
   - Update the relevant requirements, business rules, data model, and API
     contract to reflect the recorded invoice policies.

2. [x] **Align the API contract**
   - Compare the approved lifecycle behaviors with the existing invoice
     endpoints and schemas.
   - Add or adjust operations, request/response schemas, status codes, and
     descriptions only where the approved behavior is not represented.
   - Run OpenAPI validation and regenerate generated clients/interfaces using
     the existing Gradle tasks. Do not manually edit generated output.

3. [x] **Implement invoice calculation and creation**
   - Add focused domain/application logic for the approved invoice calculation,
     validation, and invoice-line construction.
   - Implement the administrator create-for-booking use case with
     transactional eligibility checks and persistence; enforce the
     one-invoice-per-booking invariant under concurrent requests and return
     the contract-defined conflict for duplicate/ineligible creation.
   - Allocate invoice numbers safely according to the approved numbering
     policy.
   - Add unit tests for calculation boundaries, rounding, validation, and
     duplicate/ineligible cases.

4. [x] **Implement invoice administration and lifecycle**
   - Add the repository operations required for database-backed invoice
     pagination, status filtering, and invoice detail retrieval.
   - Implement admin list/detail, manual mark-paid, send, and void operations.
   - Validate lifecycle transitions and preserve consistent errors using the
     existing Problem Details handling. Confirm that booking confirmation
     continues to require a PAID invoice.
   - Add focused application tests for filtering, missing resources, invalid
     transitions, and manual payment marking.

5. [x] **Defer invoice email notifications**
   - Invoice lifecycle notifications are deferred to FEAT-001 in
     `planning/backlog.md`, as requested.
   - Do not send email or add provider configuration in PLAN-007.

6. [x] **Verify persistence, API security, and integration behavior**
   - Add PostgreSQL 17 Testcontainers coverage for invoice creation, generated
     line persistence, total reconciliation, uniqueness, number allocation,
     paging/filtering, status updates, and transaction rollback as applicable.
   - Add API integration tests for ADMIN-only invoice management and
     USER-owned invoice retrieval, including rejection of cross-customer
     access and unauthenticated/unauthorized requests.
   - Verify concurrent create attempts cannot produce multiple invoices or
     duplicate invoice numbers. Verify booking confirmation remains blocked
     unless the associated invoice is PAID.
   - If Compose-backed testing is needed, use the existing
     `COMPOSE_DATABASE_TEST` opt-in convention and verify migration activity
     uses only the configured application database.

7. [x] **Document and close the milestone**
   - Document the initial VAT assumption, invoice calculation/lifecycle
     rules, deferred invoice notifications, and relevant Gradle commands.
   - Run OpenAPI validation/generation, the backend Gradle test/build tasks,
     and relevant Compose smoke tests when configured.
   - Update the roadmap to mark Phase 7 items complete only after the approved
     implementation and verification succeed. Set this plan to COMPLETED only
     after all acceptance criteria and Definition of Done are satisfied.

## Files and Components Affected

- `openapi/api.yaml` — invoice operations/schemas only if approved behavior
  requires contract changes.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/domain/model/InvoiceCalculation.kt`
  — exact invoice arithmetic and invoice-number formatting.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/port/out/PersistenceRepositories.kt`
  — invoice persistence query and number-allocation ports as required.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/service/**`
  — invoice creation, calculation, queries, and lifecycle use cases.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/api/**` — thin
  invoice delegate implementation and explicit API/domain mappings.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/infrastructure/persistence/**`
  — invoice queries, transactional persistence, and safe numbering support.
- `backend/src/main/resources/db/migration/V3__add_invoice_number_sequence.sql`
  — create the global invoice number sequence without modifying V1/V2.
- `backend/src/test/kotlin/de/pipplip/abfallcontainerservice/**` — invoice
  unit, persistence, API/security, and concurrency tests.
- `docs/requirements.md`, `docs/business-rules.md`, `docs/data-model.md`, and
  `docs/architecture.md` — approved policy and integration decisions.
- `planning/backlog.md` — record invoice lifecycle email notifications as
  deferred work under FEAT-001.
- `README.md` — approved local setup, configuration, and test instructions.
- `planning/roadmap.md` and this plan — status updates after successful
  implementation only.

Generated Kotlin/Angular sources are build outputs and must not be edited or
committed manually.

## Required Database Migrations

- Add a new migration to create the global invoice-number sequence. Invoice
  and invoice-line tables already exist; do not modify applied V1/V2
  migrations.
- Verify migrations target the `SPRING_DATASOURCE_URL` application database,
  not the separate Keycloak database used by Compose.

## Required API Changes

- The existing contract defines customer-owned invoice retrieval,
  administrator invoice list/detail/create, and manual mark-paid. Add admin
  send and void operations and document the approved creation/calculation
  policy. Email behavior is deferred and is not exposed by this API change.
- Regenerate the Kotlin Spring interfaces and Angular client with existing
  Gradle tasks; never manually modify generated sources.

## Security Implications

- Require ADMIN for invoice administration and USER plus booking ownership for
  customer invoice retrieval, consistent with the existing Keycloak/JWT
  resource-server configuration.
- Derive customer ownership from the verified token subject and local customer
  profile. Never trust a request-supplied customer ID.
- Avoid including personal data, tokens, credentials, or invoice contents in
  logs beyond approved operational metadata.

## Testing Requirements

- Unit-test invoice calculations against approved examples, including date
  boundaries, quantity, decimal precision, tax treatment, and all rounding
  boundaries.
- Use PostgreSQL 17 Testcontainers to test persisted invoices, line/totals
  reconciliation, constraints, paging/filtering, transactional behavior, and
  concurrent invoice creation/number allocation.
- Test API role restrictions, customer ownership, no-invoice and missing
  invoice responses, duplicate creation, lifecycle transitions, and manual
  paid marking.
- Verify that invoice creation and status transitions do not send email;
  notifications are deferred to FEAT-001.
- Run OpenAPI validation and generation, the Gradle test/build tasks, and any
  relevant existing Compose opt-in test without running migrations against
  Keycloak's database.

## Acceptance Criteria

- An authorized administrator can create at most one invoice for a booking
  when it meets the approved eligibility and timing policy.
- Invoice lines, amounts, currency, tax, and rounding follow the approved
  calculation rules and persisted totals are reproducible and reconcile.
- Invoice numbers are unique and allocated safely under concurrent creation
  according to the approved format.
- Administrators can retrieve/list invoices and perform only the approved
  invoice status transitions, including manual paid marking.
- Customers can retrieve only invoices associated with their own bookings;
  ownership is derived from the authenticated Keycloak subject.
- Invoice creation and status transitions send no email; invoice email
  notifications are deferred to FEAT-001.
- The existing booking confirmation rule continues to require a PAID invoice.
- OpenAPI-generated interfaces and models compile, PostgreSQL integration and
  API/security tests cover the behavior, and any necessary schema change is a
  new immutable Flyway migration.
- Gradle and the configured Docker Compose environment remain compatible;
  application database access remains isolated from Keycloak's database.
- Relevant documentation reflects approved decisions only, and the roadmap is
  updated only after implementation succeeds.

## Definition of Done

- All approved implementation steps and acceptance criteria are complete.
- Required OpenAPI generation/validation, Gradle build, and relevant unit,
  PostgreSQL integration, and API/security tests pass.
- The invoice workflow behaves transactionally and safely under concurrent
  requests, and database migrations are verified against the application
  database only.
- No email provider configuration or credentials are added in this milestone.
- Documentation and roadmap status match the implemented, approved behavior.
- This plan was approved before implementation and is marked COMPLETED only
  after implementation and verification succeed.

## Out of Scope

- Payment-provider integration or automated payment processing; payment
  remains manually managed as documented.
- Invoice PDF generation, which is documented as a future enhancement.
- Invoice email notifications and provider/delivery policy, deferred to
  FEAT-001 in the planning backlog.
- Frontend application work.
- Changes to Keycloak realm/client configuration or identity provisioning.
- Unapproved changes to customer, booking, pricing, VAT, tax, cancellation, or
  invoice policy.
- Changes to completed milestones or unrelated infrastructure and
  observability work.

## Decisions Recorded

- An administrator may create an invoice only for a PENDING or CONFIRMED
  booking. An ineligible booking or an existing invoice returns 409.
- Invoice net is the persisted daily-price snapshot multiplied by requested
  quantity and half-open rental days. One rental line is stored per booking;
  VAT is 19% of net and rounded per line using HALF_UP. This is the initial
  policy and requires tax-adviser confirmation before production use.
- Invoice numbers use `YYYY-######` with a global, non-resetting PostgreSQL
  sequence. The numeric part is unique system-wide; gaps are permitted.
- The only transitions are DRAFT to SENT or VOID and SENT to PAID. PAID and
  VOID are terminal.
- Invoice email notifications were explicitly deferred by the user to
  FEAT-001 in `planning/backlog.md`. No email provider, recipients, content,
  failure/retry policy, or duplicate-notification behavior is selected here.
- Mock-provider testing and a local email-capture service are not part of
  PLAN-007 because the notification feature is deferred.
