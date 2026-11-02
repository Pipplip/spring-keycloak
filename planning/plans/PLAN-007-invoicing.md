# PLAN-007: Invoicing

## Objective

Implement the Invoicing roadmap milestone on the existing Kotlin, Spring Boot,
PostgreSQL, and Keycloak foundation. Create and calculate booking invoices,
support the documented invoice lifecycle and access patterns, and implement
the approved invoice email notifications without inventing unresolved tax,
generation-timing, numbering, or delivery rules.

## Status

DRAFT

## Prerequisites

- PLAN-001 Project Setup, PLAN-002 OpenAPI, PLAN-003 Persistence, PLAN-004
  Security, PLAN-005 Customer Features, and PLAN-006 Admin Features are
  COMPLETED.
- Invoice and invoice-line domain/persistence models and database tables
  already exist. Customer invoice retrieval, admin invoice listing/detail,
  admin invoice creation for a booking, and manual mark-paid operations are
  already represented in `openapi/api.yaml`; customer retrieval is already
  implemented.
- The invoice policy and notification decisions in **Open Questions** must be
  approved and reflected in the requirements, business rules, data model, and
  API contract as applicable before implementing behavior.

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
- Implement the invoice-related email notifications explicitly approved for
  this milestone, using an approved provider and delivery/failure policy.
- Add unit, PostgreSQL integration, API/security, and notification tests;
  document configuration and local testing.
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
- Use the booking's persisted price snapshot and quantity as inputs only to the
  extent allowed by the approved calculation policy. Keep amount arithmetic
  exact in integer minor units and define tax/rounding behavior only after
  approval. Enforce that stored invoice totals reconcile with line totals and
  the approved tax calculation.
- Reuse the existing Spring Security/Keycloak configuration and role
  boundaries: administrator operations require ADMIN, and customer retrieval
  requires USER ownership derived from the verified JWT subject. Never accept a
  customer identity supplied by the caller.
- Use the existing Gradle Kotlin DSL, JDK 25 toolchain, Spring Boot 3.5.16,
  Kotlin 2.3.21, PostgreSQL 17, Flyway, Spring Data JPA, MockK, and PostgreSQL
  Testcontainers. Add a mail dependency only after the provider and delivery
  approach are approved.
- Keep local database configuration environment-driven. Existing Compose runs
  PostgreSQL 17 and Keycloak with a separate Keycloak database; application
  migrations and tests must continue to target only the application database.
  Add or change Compose services/configuration only if required by the approved
  email provider or local-development approach. Do not add credentials or
  tokens to source-controlled configuration.
- Keep PDF generation out of scope; the business rules describe it as a future
  enhancement.

## Implementation Steps

1. [ ] **Resolve and record invoice and notification policies**
   - Decide invoice creation timing and eligible booking states, including
     behavior for repeat requests, cancelled bookings, and bookings already
     having an invoice.
   - Approve the calculation inputs and formula, tax/VAT policy, treatment of
     quantity and rental days, line composition, and rounding at line and
     invoice level.
   - Decide invoice-number format/allocation and the permitted invoice status
     transitions, including how an invoice reaches SENT or VOID if those
     states are required.
   - Decide which invoice events send notifications, recipients/content,
     provider, delivery failure/retry behavior, and whether local Compose
     should include a development mail service.
   - Update only the relevant requirements, business rules, data model, and
     API contract to reflect approved decisions. Do not treat unresolved items
     as implementation defaults.

2. [ ] **Align the API contract**
   - Compare the approved lifecycle and notification-triggered behaviors with
     the existing invoice endpoints and schemas.
   - Add or adjust operations, request/response schemas, status codes, and
     descriptions only where the approved behavior is not represented.
   - Run OpenAPI validation and regenerate generated clients/interfaces using
     the existing Gradle tasks. Do not manually edit generated output.

3. [ ] **Implement invoice calculation and creation**
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

4. [ ] **Implement invoice administration and lifecycle**
   - Add the repository operations required for database-backed invoice
     pagination, status filtering, and invoice detail retrieval.
   - Implement admin list/detail and manual mark-paid operations, plus any
     additional lifecycle operations approved in step 1.
   - Validate lifecycle transitions and preserve consistent errors using the
     existing Problem Details handling. Confirm that booking confirmation
     continues to require a PAID invoice.
   - Add focused application tests for filtering, missing resources, invalid
     transitions, and manual payment marking.

5. [ ] **Implement and verify invoice notifications**
   - Integrate the approved email provider through an infrastructure boundary
     and environment-driven configuration; keep provider-specific code out of
     domain and API layers.
   - Trigger notifications only for the approved events and define behavior
     for delivery failures according to the approved policy; do not silently
     report a successful send when delivery fails.
   - Add provider-adapter tests and application tests for event triggering and
     failure handling. If a local mail service is approved, configure it
     without affecting the existing PostgreSQL/Keycloak database separation.

6. [ ] **Verify persistence, API security, and integration behavior**
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

7. [ ] **Document and close the milestone**
   - Document approved invoice calculation/lifecycle rules, provider
     configuration, local mail testing, and relevant Gradle commands.
   - Run OpenAPI validation/generation, the backend Gradle test/build tasks,
     and relevant Compose smoke tests when configured.
   - Update the roadmap to mark Phase 7 items complete only after the approved
     implementation and verification succeed. Set this plan to COMPLETED only
     after all acceptance criteria and Definition of Done are satisfied.

## Files and Components Affected

- `openapi/api.yaml` — invoice operations/schemas only if approved behavior
  requires contract changes.
- `backend/build.gradle.kts` — mail-provider/test dependencies only if
  required by the approved integration.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/domain/model/Invoice.kt`
  — domain invariants or calculation types if needed by approved policy.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/port/out/PersistenceRepositories.kt`
  — invoice persistence query and number-allocation ports as required.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/service/**`
  — invoice creation, calculation, queries, and lifecycle use cases.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/api/**` — thin
  invoice delegate implementation and explicit API/domain mappings.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/infrastructure/persistence/**`
  — invoice queries, transactional persistence, and safe numbering support.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/infrastructure/**`
  — approved email-provider adapter and configuration.
- `backend/src/main/resources/db/migration/V*__*.sql` — new migration only if
  approved schema changes are necessary; existing migrations remain immutable.
- `backend/src/main/resources/application.yml` — environment-driven
  configuration only as required for the approved provider.
- `backend/src/test/kotlin/de/pipplip/abfallcontainerservice/**` — invoice
  unit, persistence, API/security, concurrency, and notification tests.
- `docs/requirements.md`, `docs/business-rules.md`, `docs/data-model.md`, and
  `docs/architecture.md` — approved policy and integration decisions.
- `compose.yaml` and `.env.example` — only if the approved local mail/testing
  setup requires additional service configuration; no real secrets.
- `README.md` — approved local setup, configuration, and test instructions.
- `planning/roadmap.md` and this plan — status updates after successful
  implementation only.

Generated Kotlin/Angular sources are build outputs and must not be edited or
committed manually.

## Required Database Migrations

- No schema change is assumed: invoice and invoice-line tables, integer-cent
  monetary columns, statuses, unique invoice number, one-invoice-per-booking
  constraint, and required relationships already exist.
- If an approved policy requires new persisted fields, indexes, or constraints,
  add a new versioned Flyway migration. Never modify the applied V1/V2
  migrations.
- Verify migrations target the `SPRING_DATASOURCE_URL` application database,
  not the separate Keycloak database used by Compose.

## Required API Changes

- The existing contract already defines customer-owned invoice retrieval,
  administrator invoice list/detail, administrator invoice creation for a
  booking, and manual mark-paid.
- Change the contract first if approved status transitions or notification
  behavior require additional operations or fields. Do not invent API
  semantics for unresolved timing, tax, invoice status, numbering, or email
  rules.
- Regenerate the Kotlin Spring interfaces and Angular client with existing
  Gradle tasks; never manually modify generated sources.

## Security Implications

- Require ADMIN for invoice administration and USER plus booking ownership for
  customer invoice retrieval, consistent with the existing Keycloak/JWT
  resource-server configuration.
- Derive customer ownership from the verified token subject and local customer
  profile. Never trust a request-supplied customer ID.
- Avoid including personal data, tokens, credentials, or invoice contents in
  logs beyond approved operational metadata. Keep SMTP/provider credentials
  environment-driven and out of tracked files.
- Ensure email recipient addresses come from the trusted persisted customer
  profile, subject to the approved notification policy.

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
- Test notification triggering, recipients, provider configuration, and
  delivery failure/retry behavior using an isolated provider test or approved
  local mail service; do not depend on a real external mail account.
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
- Approved invoice notification events use the selected provider and the
  documented error/retry behavior; no unapproved notifications are sent.
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
  PostgreSQL integration, API/security, and notification tests pass.
- The invoice workflow behaves transactionally and safely under concurrent
  requests, and database migrations are verified against the application
  database only.
- Local provider configuration and test instructions are documented without
  committing secrets.
- Documentation and roadmap status match the implemented, approved behavior.
- This plan was approved before implementation and is marked COMPLETED only
  after implementation and verification succeed.

## Out of Scope

- Payment-provider integration or automated payment processing; payment
  remains manually managed as documented.
- Invoice PDF generation, which is documented as a future enhancement.
- Frontend application work.
- Changes to Keycloak realm/client configuration or identity provisioning.
- Unapproved changes to customer, booking, pricing, VAT, tax, cancellation, or
  invoice policy.
- Changes to completed milestones or unrelated infrastructure and
  observability work.

## Open Questions

- When may an invoice be created, and which booking statuses are eligible?
  Should creation be manually initiated by an administrator or automatic at a
  particular workflow event? What should happen for cancelled bookings,
  existing invoices, and repeated create requests?
- What exact invoice formula applies to daily price, rental days, and requested
  quantity? Which VAT/tax rules, rates, taxable base, and rounding policy
  apply? How should line items be composed?
- What is the invoice-number format and allocation policy (including reset
  period, uniqueness scope, and concurrency requirements)?
- Which status transitions are allowed among DRAFT, SENT, PAID, and VOID?
  Should SENT and VOID be manually actionable, and should additional API
  operations be added for them?
- Which invoice events trigger emails, who receives each notification, and
  what content is required? Which email provider should be used, and what are
  the delivery failure, retry, and duplicate-notification expectations?
- Should the local Docker Compose environment include an email-capture service
  for development and integration testing, or should email testing use a
  mocked provider only?
