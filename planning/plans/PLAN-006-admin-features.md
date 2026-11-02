# PLAN-006: Admin Features

## Objective

Implement the Admin Features milestone in `planning/roadmap.md`: administrator
booking management, physical container inventory, container-type management,
and status management. Use the existing Kotlin/Spring Boot application,
PostgreSQL persistence, Keycloak ADMIN authorization, and API-first OpenAPI
contract while preserving booking allocation and inventory integrity.

## Status

COMPLETED

## Prerequisites

- PLAN-001 Project Setup, PLAN-002 OpenAPI, PLAN-003 Persistence, PLAN-004
  Security, and PLAN-005 Customer Features are COMPLETED.
- The Security milestone already enforces `ROLE_ADMIN` for `/api/v1/admin/**`
  and configures the local Keycloak realm with the `ADMIN` role.
- The OpenAPI specification already defines administrator routes for container
  types, physical containers, customers, bookings, booking assignments, and
  invoices. Customer administration is approved for this milestone even though
  it is not listed in the Admin Features roadmap checklist; invoice
  administration remains deferred to the Invoicing milestone.
- Existing PostgreSQL schema and domain models cover customers, container
  types, containers, bookings, assignments, and invoices. Customer Features
  already creates bookings and assignments transactionally, and customer
  cancellation releases assignments.
- The Open Questions below have approved answers, which are recorded in the
  relevant business, requirements, data-model, and API documentation.

## Scope

- Implement the existing ADMIN-protected container-type list, detail, create,
  and update operations. Include inactive types in administrative reads and
  support activation/deactivation through the existing `active` field.
- Implement the existing ADMIN-protected physical-container list, detail,
  create, and update operations, including filtering by container status and
  type.
- Implement the existing ADMIN-protected booking list with the contract's
  filters, booking detail, and booking-status update operations.
- Implement administrator customer listing, detail, and profile update
  operations, without exposing or changing the Keycloak subject.
- Implement administrator listing, assignment, and release of physical
  containers for a booking, validating type compatibility, container
  eligibility, interval conflicts, and booking/assignment relationships
  according to the policies approved before implementation.
- Use the existing zero-based page and size contract (size 1–100) and database
  paging for list operations.
- Preserve the generated OpenAPI delegate pattern, explicit API/domain
  mapping, clean architecture boundaries, and RFC 7807 Problem Details errors.
- Add unit, PostgreSQL integration, API, and authorization tests for the
  operations and policies approved for this milestone.
- Do not add a frontend application or assume that Docker Compose runs the
  backend service.

## Technical Approach

- Keep `openapi/api.yaml` as the API source of truth. The Admin routes and
  request/response models already exist; do not change the contract unless an
  approved requirement exposes a mismatch. Regenerate outputs with the
  existing Gradle OpenAPI tasks and never edit generated sources manually.
- Keep generated API delegates thin. Add application-level admin query and
  command services, extend output ports as needed, and implement them in the
  existing persistence adapters. Map API DTOs explicitly to/from domain models;
  do not use JPA entities as API models.
- Reuse existing domain entities, repositories, status enums, booking
  assignment behavior, Keycloak realm-role conversion, `/api/v1/admin/**`
  security rule, and RFC 7807 handling. Do not duplicate customer booking
  allocation or weaken USER ownership checks.
- Implement pageable repository queries with stable ordering for container
  types, containers, bookings, and assignments. Apply filters in PostgreSQL
  rather than loading complete tables into memory. Validate date filter bounds
  and pagination inputs at the application/API boundary.
- Keep booking/assignment mutations transactional. Preserve PostgreSQL's
  existing half-open interval overlap trigger and row-locking strategy for
  container allocation; translate only known domain/database conflicts to the
  contract's 409 response and allow unexpected failures to surface.
- Validate changes to container type, active state, and physical status against
  approved policy, particularly when existing bookings or active assignments
  are affected. Do not infer lifecycle rules from CRUD request shapes alone.
- The existing schema is expected to be sufficient. Add a new Flyway migration
  only if approved implementation requirements need a new constraint or
  index; never modify an applied migration or run application migrations on
  Keycloak's separate database.
- Use the existing Gradle Kotlin DSL, JDK 25 toolchain, Spring Boot 3.5.16,
  Kotlin 2.3.21, PostgreSQL 17 Testcontainers, JUnit 5, and MockK. No new
  dependency is expected.
- Use Testcontainers for ordinary PostgreSQL-specific tests. Compose provides
  PostgreSQL and Keycloak but no backend service; any Compose database smoke
  test remains opt-in using `COMPOSE_DATABASE_TEST=true`. Preserve the separate
  application and Keycloak databases and use environment-provided credentials
  only.

## Implementation Steps

1. [x] **Approve scope and admin lifecycle policies**
   - Customer list/detail/update operations are included. Invoice
     administration, including list/detail/create/mark-paid, remains deferred
     to the Invoicing milestone.
   - Allowed status transitions are `PENDING` to `CONFIRMED` or `CANCELLED`,
     `CONFIRMED` to `DELIVERED` or `CANCELLED`, and `DELIVERED` to `COMPLETED`;
     `CANCELLED` and `COMPLETED` are terminal. Confirmation requires an
     existing PAID invoice. Cancellation releases all active assignments.
     Administrator status changes send no notifications.
   - Manual assignment and release are allowed only for `PENDING` and
     `CONFIRMED` bookings and do not change booking status. Assigning more
     containers than requested is allowed, logged, and visible through the
     derived `assignedContainerCount` Booking API field; the requested booking
     quantity is unchanged. Released assignments remain in history.
   - A physical container's type can be changed only when it has no unreleased
     assignment for a non-cancelled booking whose pickup date is in the future.
     Changing its status to `MAINTENANCE` or `RETIRED` has the same restriction.
   - A container type may be deactivated or have its details changed only when
     no existing booking references it. Booking price snapshots remain
     unchanged.
   - No additional booking sort fields or customer search filters are needed.
     These decisions are reflected in project documentation and OpenAPI.

2. [x] **Implement container-type administration**
   - Add application use cases and persistence queries for paginated
     administrative list/detail, create, and update operations.
   - Include active and inactive types; apply the optional `active` filter.
     Validate required values and preserve existing domain/database
     constraints for volume, currency, and non-negative price.
   - Add focused unit and PostgreSQL/API tests for create/update, inactive
     visibility, filters, pagination, missing IDs, and invalid input.

3. [x] **Implement physical-container inventory administration**
   - Add application use cases and persistence queries for paginated
     list/detail, create, and update operations, with optional type and status
     filters.
   - Enforce unique inventory numbers and require a valid existing container
     type. Apply only the approved type/status changes policy where assignments
     or bookings exist.
   - Add unit, PostgreSQL, and API tests for filters, paging, duplicate
     inventory numbers, missing types/containers, and assignment-sensitive
     updates.

4. [x] **Implement administrator booking queries**
   - Add paginated booking search with status, customer, container-type, and
     inclusive delivery-date filters from the contract, plus administrator
     booking detail.
   - Use stable database-backed paging and validate invalid date bounds and
     pagination. Return only the contract's Booking model; do not expose
     Keycloak subjects or persistence entities.
   - Add tests for combinations of filters, date boundaries, paging, unknown
     bookings, and response mapping.

5. [x] **Implement administrator assignment operations**
   - Add paginated assignment listing for a booking and implement manual
     assignment and release operations using the approved assignment policies.
   - Validate that referenced booking, assignment, and container belong to the
     expected relationships; enforce container type/status eligibility and
     prevent overlapping allocations transactionally.
   - Preserve released assignment history with `releasedAt`; avoid silently
     changing booking quantity or status unless explicitly approved.
   - Add PostgreSQL integration tests for matching/mismatched types,
     unavailable containers, overlapping and touching date ranges, repeated
     releases, assignment ownership within the booking path, transaction
     rollback, and concurrent conflicts.

6. [x] **Implement administrator booking status management**
   - Add an application command that validates the approved transition graph
     and executes the transition transactionally, including any approved
     assignment release or related side effects.
   - Preserve booking version/optimistic-lock behavior and return the updated
     booking. Translate invalid transitions or concurrent updates to the
     contract's conflict response.
   - Add unit and PostgreSQL/API tests for each approved transition, forbidden
     transitions, missing bookings, concurrent updates, and side effects.

7. [x] **Wire API delegates and verify authorization/error behavior**
   - Implement generated Administration delegate operations for the approved
     milestone scope, with explicit mappings and appropriate 201/200
     responses.
   - Include the approved customer list, detail, and update operations. Never
     expose or allow updates to Keycloak subject values.
   - Confirm all routes require ADMIN under the existing SecurityFilterChain;
     USER tokens receive 403 and missing/invalid tokens receive 401.
   - Verify validation, not-found, conflict, and unexpected error handling
     conform to the generated contract and existing Problem Details behavior.

8. [x] **Verify and document the milestone**
   - Run targeted Gradle tests and the backend build using JDK 25. Validate and
     regenerate Kotlin and Angular OpenAPI clients if the approved contract
     changes.
   - Run PostgreSQL-specific tests with Testcontainers. Optionally run the
     Compose PostgreSQL smoke test with `COMPOSE_DATABASE_TEST=true`; do not
     target Keycloak's database.
   - Update applicable business/API/data-model documentation, this plan's
     status, and Admin Features roadmap checkboxes only after implementation
     and verification succeed.

## Files and Components Affected

- `openapi/api.yaml` — existing admin contract is expected to suffice; change
  only after approval and regenerate generated outputs through Gradle.
- `backend/build.gradle.kts` — no dependency change expected; use existing
  OpenAPI validation/generation, test, and build tasks.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/api/**` — new
  generated Administration delegate implementation and explicit DTO/domain
  mappings.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/**` —
  admin query/command use cases and required output ports.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/domain/**` —
  lifecycle validation only where the approved business rules require it.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/infrastructure/persistence/**`
  — Spring Data queries, pageable filters, persistence adapters, and
  transactional assignment support.
- `backend/src/main/resources/db/migration/**` — no migration expected; add a
  new migration only for an approved schema/index requirement.
- `backend/src/test/kotlin/de/pipplip/abfallcontainerservice/**` — application
  unit, PostgreSQL persistence, API, authorization, status transition, and
  assignment concurrency coverage.
- `docs/requirements.md`, `docs/business-rules.md`, `docs/data-model.md`, and
  possibly `docs/architecture.md` — approved lifecycle/scope clarifications.
- `README.md` — only if administrator setup or test instructions need updating.
- `compose.yaml` — no change expected; existing PostgreSQL/Keycloak services
  and opt-in database smoke-test convention are sufficient.
- `planning/roadmap.md` and this file — status updates after successful
  implementation only.
- Generated sources under `backend/build/generated` and
  `frontend/build/generated` are build output and must not be edited manually.

## Required Database Migrations

None expected. The existing schema has the required entities, foreign keys,
uniqueness constraints, status checks, booking version, and PostgreSQL trigger
that prevents overlapping active assignments. Verify whether database-backed
admin filters need additional indexes; if an approved query requirement
justifies an index, add a new Flyway migration without changing V1 or V2.
Application migrations must never run against Keycloak's separate database.

## Required API Changes

The existing ADMIN-protected routes were implemented. The approved lifecycle
policies were added to `openapi/api.yaml`, along with the derived
`Booking.assignedContainerCount` field and documented 404/409 responses for
missing referenced types and protected updates. Kotlin and Angular generated
outputs were regenerated from the contract. The implemented operations are:

- Container-type administration: list/filter, create, detail, and update.
- Physical-container administration: list/filter, create, detail, and update.
- Booking administration: list/filter, detail, status update, assignment list,
  assignment creation, and assignment release.
- Customer administration: list, detail, and profile update.

Invoice administration remains in the later Invoicing milestone. No database
migration was needed; existing tables, constraints, row locking, and the
assignment-overlap trigger support this implementation.

## Security Implications

- Preserve ADMIN-only access to `/api/v1/admin/**` using the existing validated
  Keycloak JWT and realm-role mapping. Do not rely solely on generated
  OpenAPI annotations.
- Test unauthorized, USER, and ADMIN access for every implemented route;
  preserve 401 versus 403 semantics.
- Do not expose Keycloak subject values in customer information, logs, or API
  responses. If customer administration is approved into this milestone, use
  only the existing customer API model that omits that subject.
- Apply domain and persistence constraints in addition to request validation.
  Never trust request identifiers to establish unrelated ownership or
  assignment relationships.
- Keep assignment changes transactional and preserve PostgreSQL overlap
  protection. Do not convert database failures into success-shaped responses.
- Keep PostgreSQL and Keycloak databases isolated; credentials remain
  environment-driven and must not be committed.

## Testing Requirements

- Unit-test admin use cases with JUnit 5 and MockK, including filters,
  validation, not-found behavior, and every approved status/assignment rule.
- Use PostgreSQL 17 Testcontainers for paging/filter queries, uniqueness and
  foreign-key constraints, transactional mutations, optimistic locking, and
  overlap-trigger behavior. Do not use an in-memory database for
  PostgreSQL-specific guarantees.
- Test all generated delegate routes and explicit mapping, including 400, 404,
  and 409 Problem Details responses.
- Verify no credentials, Keycloak subjects, internal entities, or unapproved
  fields are exposed through admin responses.
- Verify role access: anonymous/invalid credentials return 401, authenticated
  USER returns 403, and ADMIN can access only the documented admin functions.
- Verify assignment operations for matching container type, AVAILABLE status,
  time-range overlap, touching intervals, released assignments, repeated
  release, and concurrent conflicting assignment attempts.
- Run the relevant Gradle unit/integration tests and build with the configured
  JDK 25 toolchain. If the OpenAPI contract changes, run `openApiValidate`,
  `generateKotlinOpenApi`, `generateAngularOpenApi`, and
  `validateAngularOpenApiOutput`.
- Keep Compose database connectivity tests opt-in with
  `COMPOSE_DATABASE_TEST=true`; verify the application datasource does not use
  Keycloak's database.

## Acceptance Criteria

- ADMIN users can list, filter, retrieve, create, and update container types
  and physical containers using the existing OpenAPI contract; inactive types
  are visible to admins and their public catalogue behavior is unchanged.
- ADMIN users can list/filter and retrieve bookings and change booking status
  only according to the approved transition policy.
- ADMIN users can list, assign, and release booking container assignments
  according to the approved policies; inventory is not double-allocated and
  assignment history is retained.
- ADMIN users can list, retrieve, and update customer profiles without
  exposing or changing Keycloak subject values.
- Customer and administrator Booking responses expose the current unreleased
  assignment count, including approved over-allocation.
- Lists use stable database-backed pagination and honor documented filters and
  page bounds.
- Invalid input, missing records, conflicts, and authorization failures produce
  the documented status and Problem Details responses.
- No customer ownership boundary, public catalogue behavior, Customer
  Features behavior, or Keycloak database isolation is weakened.
- No applied migration or generated source is manually changed; any required
  schema change is additive through a new Flyway migration.
- Relevant unit, PostgreSQL integration, API/security, OpenAPI validation, and
  Gradle build checks pass.
- Applicable docs and roadmap status reflect only approved, verified behavior.

## Definition of Done

- All Open Questions affecting implementation have explicit approved answers
  documented in the relevant project materials.
- Every approved implementation step and acceptance criterion is complete.
- Gradle build, relevant unit tests, PostgreSQL integration tests, API/security
  tests, and OpenAPI validation/generation checks pass.
- Booking status and assignment mutations enforce approved lifecycle rules
  transactionally and maintain inventory integrity under concurrent requests.
- Admin routes conform to OpenAPI and remain protected by the existing Keycloak
  ADMIN role boundary.
- Documentation reflects implemented policies; `planning/roadmap.md` is
  updated and this plan is marked COMPLETED only after verification succeeds.

## Out of Scope

- Invoice listing, retrieval, creation, calculation, status updates, or
  manually marking invoices paid; these belong to the Invoicing milestone.
- Keycloak realm/client/role changes, profile provisioning, customer
  self-registration, or customer ownership changes.
- Payment-provider integration, VAT policy, invoice-generation timing, PDF
  generation, and email notifications.
- Angular UI implementation; the repository currently generates and validates
  an Angular client but does not contain an Angular application.
- Application Dockerfile, adding a backend service to Docker Compose,
  production deployment, CI/CD, monitoring, or unrelated roadmap milestones.
- New database entities or changes to completed setup, persistence, security,
  or Customer Features behavior except where an approved Admin requirement
  proves a change necessary.

## Open Questions

- Should customer list/detail/update operations be included in this milestone?
  They are in FR-006 and the existing OpenAPI contract, but omitted from the
  roadmap's Admin Features checklist. - YES include customer administration in this milestone
- Confirm invoice administration remains wholly in the Invoicing milestone,
  including the already contracted list/detail/create/mark-paid operations. - Confirm
- What booking status transitions are allowed, and what evidence permits
  `PENDING` to become `CONFIRMED` given the documented payment gate and
  deferred invoice/payment work? - The admin checks manually if there is a valid invoice and payment before confirming the booking. Allowed transitions: PENDING to CONFIRMED or CANCELLED, CONFIRMED to DELIVERED or CANCELLED, DELIVERED to COMPLETED; CANCELLED and COMPLETED are terminal.
- Should an administrator transition to `CANCELLED` release active assignments? - YES, canceling a booking should release all active assignments
  Which status changes trigger notifications, if any? - No notifications are sent by the admin, only by the system for customer actions
- May administrators create partial assignments, assign more containers than
  the booking's requested quantity, or release an assignment while the booking
  remains active? What should happen if the resulting assignment count no
  longer matches the booking quantity? - The booking quantity does not change; administrators may assign more than requested. The system allows and logs over-assignment, and exposes the unreleased assignment count on the Booking API response.
- Which booking statuses allow manual assignment/release, and should
  assignment operations change booking status? - Manual assignment/release is allowed for PENDING and CONFIRMED bookings. Assignment operations do not change booking status.
- May a physical container's type or status be changed when it has active or
  future assignments? Should a transition to MAINTENANCE or RETIRED be rejected
  or otherwise handled when reservations exist? - A physical container's type can be changed only if it has no active or future assignments. Changing the status to MAINTENANCE or RETIRED should be rejected if there are active or future assignments.
- May a container type be deactivated or have its price/details changed while
  existing bookings reference it? Historical booking price snapshots are
  already stored and must remain unchanged. - A container type can be deactivated or have its price/details changed only if there are no existing bookings referencing it. Historical booking price snapshots must remain unchanged.
- Are additional booking list sort fields or customer search filters required
  beyond the existing OpenAPI parameters? - No additional sort fields or customer search filters are required beyond the existing OpenAPI parameters.
- Which status transitions trigger notifications? - No notifications are sent for administrator changes; system notifications remain limited to customer actions.
