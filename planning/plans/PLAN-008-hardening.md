# PLAN-008: Hardening

## Objective

Harden the existing Kotlin and Spring Boot API against invalid requests,
inconsistent error responses, inadequate operational diagnostics, and
pagination edge cases. Keep behavior aligned with the OpenAPI contract while
expanding automated verification across the application, PostgreSQL, Keycloak
security boundary, and the available Docker Compose development environment.

## Status

COMPLETED

## Prerequisites

- PLAN-001 through PLAN-007 are COMPLETED.
- The API contract is `openapi/api.yaml`; generated Kotlin interfaces/models
  are build output and must not be edited manually.
- The existing API already has input validation in application services,
  RFC 7807 `ProblemDetail` responses for known API/security errors, pagination
  range checks (`page >= 0`, `size` from 1 to 100) and some overflow guards,
  PostgreSQL 17 Testcontainers tests, and an opt-in Compose PostgreSQL
  connection test.
- `CustomerOwnershipExceptionHandler` handles known validation, not-found,
  conflict, and ownership cases. `SecurityConfiguration` provides RFC 7807
  authentication and authorization responses. The only application log
  currently identified is a warning for administrator over-assignment.
- Compose currently runs PostgreSQL and Keycloak, with Keycloak using a
  separate database. It does not yet run a backend container; deployment
  packaging is planned for Phase 10.

## Scope

- Audit and strengthen API input validation without adding undocumented
  business rules or changing approved booking, inventory, customer, or invoice
  behavior.
- Verify that known client errors and unexpected server errors are represented
  consistently and safely as RFC 7807 Problem Details.
- Improve backend operational logging while preventing tokens, credentials,
  request bodies, and unnecessary personal data from appearing in logs.
- Verify pagination bounds, overflow handling, deterministic ordering, and
  response metadata across all paginated API operations.
- Extend unit, PostgreSQL integration, API, and security tests for validation,
  error handling, logging-sensitive behavior, and pagination.
- Verify the existing Gradle test/build and OpenAPI-generation paths, plus the
  opt-in Compose database path where configured.
- Update developer documentation and the roadmap only after implementation
  and verification succeed.

## Technical Approach

- Preserve Gradle Kotlin DSL, the configured JDK 25 toolchain, Kotlin 2.3.21,
  Spring Boot 3.5.16, and the existing JUnit 5, MockK, Spring Security test,
  and Testcontainers dependencies. Do not add a lint or logging dependency
  without resolving the open question below.
- Treat `openapi/api.yaml` as the source of truth. It already specifies
  pagination bounds and common Problem Details schemas; compare runtime
  behavior to that contract and modify it only if the approved hardening
  behavior is not described. Regenerate generated outputs using existing
  Gradle tasks and do not edit generated source files.
- Reuse service-level domain validation and the existing exception advice.
  Keep client-facing error details generic and safe; do not expose exception
  messages, SQL details, stack traces, or security internals in responses.
  If a fallback for unexpected server errors is required, log the failure
  internally and return a generic 500 Problem Detail rather than catching and
  suppressing errors.
- Keep logging in the existing SLF4J/Spring logging path. Log operationally
  useful events and unexpected failures, but do not log bearer tokens,
  credentials, request bodies, or unnecessary personal/customer data.
- Retain the contract's page range (`page >= 0`, `size` from 1 to 100) and
  make overflow handling consistent across all paginated operations. Check
  every paginated repository query for deterministic ordering and avoid
  changing filtering or ordering semantics unless required to correct a
  demonstrated defect.
- Use PostgreSQL 17 Testcontainers for database-backed behavior. Test JWT
  authorization with the existing controlled test decoder/JWKS setup instead
  of requiring an external Keycloak service in the ordinary Gradle test suite.
  Where Compose smoke testing is enabled, use the existing
  `COMPOSE_DATABASE_TEST=true` opt-in and verify application database
  isolation from Keycloak's database.
- Do not add migrations unless an identified, approved requirement needs a
  schema change; any such change must be a new Flyway migration and must
  never alter an applied migration.

## Implementation Steps

1. [x] **Audit hardening behavior against the contract**
   - Inventory request validations, exception mappings, logging sites, and
     paginated operations against `openapi/api.yaml`.
   - Record gaps and retain already-correct behavior; do not duplicate
     validation or introduce new business policies.
   - Resolve any behavior that changes API semantics through the open
     questions before implementing it.

2. [x] **Complete request-validation coverage**
   - Exercise required, malformed, out-of-range, and semantically invalid
     request fields at the API boundary, including dates, quantities,
     identifiers, enum/query values, and request bodies where applicable.
   - Confirm malformed JSON, missing parameters, type mismatches, and bean
     validation failures produce the contract's 400 Problem Detail shape.
   - Keep validation aligned with OpenAPI constraints and current service
     rules; add contract constraints only where approved and currently absent.

3. [x] **Harden API error responses**
   - Verify the existing mappings for 400, 401, 403, 404, and 409 across
     controller advice and Spring Security, including the
     `application/problem+json` media type and status/title consistency.
   - Ensure unexpected server failures do not expose internal exception,
     database, or security details to API clients. If required, add a narrowly
     scoped fallback that logs the failure and returns a generic 500 Problem
     Detail; do not catch errors and continue as if successful.
   - Add or update the OpenAPI error responses only if the agreed runtime
     behavior is not represented by the contract, then regenerate and compile
     generated Kotlin interfaces.

4. [x] **Improve safe operational logging**
   - Add logging at appropriate operational boundaries for unexpected
     failures and relevant exceptional conditions not already observable.
   - Preserve the existing over-assignment warning and avoid duplicate or
     high-volume logs for normal client errors.
   - Verify log messages and structured context do not include credentials,
     tokens, request payloads, or unnecessary personal/customer information.

5. [x] **Verify and harden pagination**
   - Apply boundary tests to every paginated route: default values,
     zero-based page validation, size 1 and 100 acceptance, and rejection of
     negative pages, size 0, size above 100, and arithmetic overflow where
     the API/runtime can receive it.
   - Verify all repository queries have deterministic ordering, including
     stable tie-breakers for timestamp-based ordering.
   - Verify returned item counts and page metadata remain correct for empty,
     partial, full, and out-of-range pages; preserve documented filter
     behavior.

6. [x] **Expand regression and integration tests**
   - Add focused unit tests for validation and exception mapping, plus
     Spring MVC/security integration tests for representative malformed and
     invalid requests and safe error responses.
   - Add PostgreSQL Testcontainers coverage for paginated data and ordering,
     including relevant filters and page metadata.
   - Re-run regression coverage for booking concurrency/ownership,
     administrator authorization, invoice lifecycle, and persistence
     constraints to ensure hardening does not change established behavior.
   - Keep any Compose-backed test opt-in; verify it connects only to the
     configured application database and does not apply Flyway migrations to
     Keycloak's database.
   - The opt-in Compose smoke test was not run because no local `.env`
     configuration is present; PostgreSQL Testcontainers verification passed.

7. [x] **Document and close the milestone**
   - Update `README.md` with the verified Gradle test/build and optional
     Compose test commands, and document stable API error and pagination
     behavior where useful to contributors.
   - Run OpenAPI validation/generation as applicable, the Gradle test and
     build tasks, and the Compose smoke test when configured.
   - Mark Phase 8 items and this plan complete only after all acceptance
     criteria and the Definition of Done pass.

## Files and Components Affected

- `openapi/api.yaml` — only if the audit identifies a contract mismatch or an
  approved error response/validation constraint is missing.
- `backend/build.gradle.kts` — only if an approved tooling decision requires a
  dependency or verification task; no new tooling is assumed by this plan.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/service/**`
  — existing request and pagination validation, only where tests establish a
  gap.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/security/CustomerOwnershipExceptionHandler.kt`
  and `SecurityConfiguration.kt` — consistent and safe error behavior.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/infrastructure/persistence/PersistenceAdapters.kt`
  — deterministic pagination ordering only where the audit identifies a gap.
- `backend/src/test/kotlin/de/pipplip/abfallcontainerservice/**` — request
  validation, API errors, pagination, security, and PostgreSQL regression
  coverage.
- `compose.yaml` — only if a verified Compose test or configuration gap
  requires a minimal change; no backend service or deployment stack is
  assumed in this milestone.
- `README.md` — verified hardening test and local Compose instructions.
- `planning/roadmap.md` and this plan — status updates after successful
  implementation only.

Generated Kotlin/Angular sources are build outputs and must not be edited
manually.

## Required Database Migrations

None expected. The milestone is intended to harden API validation, error
handling, logging, and read pagination without changing the data model. If an
approved implementation requirement proves a schema change necessary, add a
new immutable Flyway migration and verify it targets only the configured
application database.

## Required API Changes

None expected before the contract audit. Existing pagination parameters
specify a zero-based page and a page size from 1 to 100; common error schemas
use Problem Details. If the agreed 500 behavior or any required validation is
not represented, update `openapi/api.yaml`, validate it, and regenerate
generated outputs. Never edit generated sources manually.

## Security Implications

- Preserve the existing Keycloak JWT issuer, signature, expiration, and
  audience validation, plus the USER/ADMIN authorization and customer
  ownership boundaries.
- Do not expose internal exception messages, stack traces, SQL details, tokens,
  credentials, or security implementation details in HTTP responses.
- Ensure logs do not include credentials, bearer tokens, raw request bodies,
  or unnecessary customer data.
- Exercise API security behavior with the controlled test JWT/JWKS setup. Use
  Compose/Keycloak smoke checks only when explicitly configured; do not change
  realm/client policy or provisioning behavior in this milestone.
- Preserve the separate Keycloak database and ensure the application's
  datasource and Flyway continue to target only the application database.

## Testing Requirements

- Use the existing Gradle wrapper, JDK 25 toolchain, JUnit 5, MockK, Spring
  Security test support, and PostgreSQL 17 Testcontainers.
- Test OpenAPI-declared field and pagination constraints, malformed request
  handling, and validation across the public, USER, and ADMIN API surfaces.
- Verify RFC 7807 response body, media type, status, and safe detail for
  expected client/security errors and unexpected server errors.
- Verify that internal failures are observable in server logs without
  recording credentials, bearer tokens, request bodies, or unnecessary
  personal data.
- Test every paginated route for accepted and rejected bounds, stable
  ordering, filtering, and accurate page metadata.
- Re-run existing security/ownership, booking, administration, invoice, and
  persistence regression tests.
- Run OpenAPI validation and relevant generation/compile tasks, the Gradle
  test and build tasks, and the Compose database test when the local Compose
  environment is intentionally configured.

## Acceptance Criteria

- Invalid and malformed requests are rejected as 400 Problem Details in
  accordance with the OpenAPI contract and existing business rules.
- Expected authentication, authorization, not-found, and conflict conditions
  retain their correct 401, 403, 404, and 409 semantics and do not leak
  internal details.
- Unexpected server failures return a safe generic 500 Problem Detail and are
  logged for operators without being swallowed or represented as success.
- Logs contain useful operational context while excluding credentials,
  access/refresh tokens, request bodies, and unnecessary personal/customer
  data.
- Every paginated operation enforces the documented `page >= 0` and
  `1 <= size <= 100` range, avoids overflow, uses deterministic ordering, and
  reports correct metadata.
- Existing authorization, customer ownership, booking allocation, invoice,
  and persistence behavior remains unchanged and is covered by passing
  regression tests.
- The Gradle/OpenAPI build path passes, PostgreSQL integration tests pass, and
  any opt-in Compose database test confirms application/Keycloak database
  isolation.
- No applied migration is changed, and generated API sources are not manually
  edited.
- Documentation and Phase 8 roadmap status match the approved, verified
  implementation.

## Definition of Done

- All approved implementation steps and acceptance criteria are complete.
- Required OpenAPI validation/generation, Gradle tests/build, and
  PostgreSQL-backed integration tests pass.
- Error responses remain RFC 7807-compatible, safe, and consistent with
  authentication/authorization semantics.
- Logs provide useful diagnostics without exposing secrets or unnecessary
  personal data.
- Pagination is bounded, deterministic, and verified across API operations.
- Compose-backed verification, when configured, confirms the correct
  application database is used and Keycloak's database remains isolated.
- Documentation and roadmap status are updated only after the implementation
  is verified; this plan is marked COMPLETED only after all approved criteria
  are satisfied.

## Out of Scope

- New product or business rules for booking, availability, pricing, VAT,
  cancellation, inventory, customers, or invoices.
- Changes to Keycloak realm/client configuration, token policy, or profile
  provisioning.
- Angular application implementation or frontend-side error handling.
- New database entities, schema redesign, or changes to applied Flyway
  migrations.
- Adding a backend Dockerfile/service, production deployment configuration,
  health/readiness endpoints, CI pipeline, or full deployment hardening;
  these belong to Phase 10 unless separately approved.
- Introducing a metrics/tracing platform, a new logging stack, or unapproved
  Gradle lint/static-analysis tooling.
- Invoice email notifications, which remain deferred to FEAT-002.

## Open Questions

- Should the OpenAPI contract declare the generic 500 Problem Details response
  on every operation, or through a shared/default response convention? - Use a shared default response if possible, but do not change the contract unless the agreed runtime behavior is not represented.
- Are there approved operational requirements for structured log fields,
  request/correlation identifiers, or log retention beyond safe SLF4J
  application logging? - If so, implement them; otherwise, keep the existing logging path and avoid adding a new logging dependency.
- Should this milestone add Kotlin static-analysis/formatting tooling to
  Gradle, or rely on the current compiler, tests, and review process? - If so, add it; otherwise, keep the existing Gradle build and review process.
