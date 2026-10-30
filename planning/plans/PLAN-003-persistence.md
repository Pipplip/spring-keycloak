# PLAN-003: Persistence

## Objective

Implement the persistence foundation for the documented domain using Kotlin, Spring Boot, Spring Data JPA, PostgreSQL, and Flyway. Define domain entities and repository boundaries, create a versioned database schema with integrity constraints, and verify it against PostgreSQL without implementing API behavior, security, or customer/booking workflows.

## Status

COMPLETED

## Prerequisites

- PLAN-001 Project Setup is COMPLETED.
- PLAN-002 OpenAPI is COMPLETED.
- Resolve the persistence-specific data-model decisions before approving this plan; see the recorded resolutions below.
- The backend currently connects to PostgreSQL using JDBC. The Compose PostgreSQL service is `postgres:17-alpine`; Keycloak uses a separate database on that service.

## Scope

- Add the Spring Boot JPA and Flyway dependencies and Kotlin/JPA support required by persistence.
- Define domain entities/value objects and persistence-layer entities/mappers for the documented customer, container type, physical container, booking, container assignment, invoice, and invoice line concepts.
- Define Spring Data repositories and the persistence adapters or repository implementations needed to keep domain/application code independent of JPA.
- Add the initial immutable Flyway migration for the application schema, foreign keys, unique/check constraints, and indexes.
- Configure and verify automatic Flyway migration against the application PostgreSQL database for local startup and tests.
- Test persistence mappings, repository behavior, schema constraints, migration repeatability, and the documented database invariants using PostgreSQL Testcontainers.
- Document migration and test commands and update the data-model documentation only for decisions explicitly approved before implementation.

## Technical Approach

- Keep the domain model and persistence model separate. Domain types belong under the architecture's `domain` layer; JPA entities, Spring Data repositories, adapters, and explicit mappers belong under `infrastructure.persistence`. Do not use generated OpenAPI DTOs as domain or JPA entities.
- Keep Spring Data/JPA types out of domain interfaces. Define repository ports in the appropriate inner layer and implement them in infrastructure, following existing package conventions and avoiding abstractions that are not needed by a real use case.
- Use UUID primary keys and PostgreSQL-compatible column types. Map `Address` as a value object/embeddable or equivalent owned fields rather than introducing an independent address aggregate/table without a documented requirement.
- Create the initial application schema in a new Flyway migration under the backend resources migration path. Once applied, migrations are immutable; schema changes require a subsequent migration. Do not have Hibernate create or update the production schema.
- Apply foreign keys, the documented uniqueness rules, status/value checks, appropriate indexes, and optimistic locking where required by the approved model. Enforce referential integrity in PostgreSQL rather than relying only on application validation.
- Preserve the data ownership boundary: `Customer.keycloakSubject` is an opaque unique identity reference, not a foreign key to Keycloak. This milestone stores customer profiles but does not call Keycloak, provision realms/users, validate JWTs, map roles, or implement ownership authorization.
- Point Flyway and JPA at the application database selected by the existing `spring.datasource` configuration. Never migrate, create tables in, or otherwise alter the separate Keycloak database. Keep credentials environment-driven.
- Retain the existing PostgreSQL Compose service and named volume. Use PostgreSQL 17 Testcontainers for deterministic integration tests, and optionally verify migrations against the running Compose database using the existing opt-in Compose-test convention. No new Docker Compose service is required.
- Preserve the API-first boundary: persistence does not change endpoints or generated output. Persisted fields and mappings must be explicitly reconciled with `openapi/api.yaml`, `docs/data-model.md`, and `docs/business-rules.md` after open questions are resolved.
- Do not implement booking allocation/use cases or claim concurrency safety based solely on a unit test. The persistence layer must support the approved database invariant for non-overlapping assignments; the exact PostgreSQL enforcement mechanism must be decided before implementation.

## Implementation Steps

1. [x] **Resolve model and database decisions**
   - Decide the persisted representation and precision/scale for all monetary values, reconciling the `Decimal` fields in `docs/data-model.md` with the integer-cent and currency-code fields in `openapi/api.yaml`.
   - Decide how booking quantity is persisted and documented; `BookingRequest` and the API `Booking` include quantity, while the current data model's `Booking` does not.
   - Decide how PostgreSQL will enforce non-overlapping active container assignments when the booking dates are stored on a related booking row.
   - Decide any additional persistence policies listed under Open Questions. Record only approved decisions in the schema and documentation.

2. [x] **Configure Kotlin/JPA and Flyway**
   - Add Spring Data JPA, Flyway core and PostgreSQL support, and Kotlin JPA compiler support using versions compatible with the existing Spring Boot 3.5.16, Kotlin 2.3.21, Gradle 9.3.0, and JDK 25 setup.
   - Retain the PostgreSQL driver and existing environment-driven datasource settings.
   - Configure Flyway as the schema authority and ensure Hibernate validates, rather than creates or updates, the schema.
   - Verify application startup applies migrations only to the configured application database.

3. [x] **Define domain and persistence models**
   - Add domain value objects, entities, and enums for Customer, ContainerType, Container, Booking, ContainerAssignment, Invoice, and InvoiceLine, using the approved data model and business rules.
   - Add JPA persistence entities, explicit domain/persistence mappers, repository ports, Spring Data repositories, and adapters in the infrastructure layer as needed.
   - Represent the documented UUID identifiers, `Customer.keycloakSubject` uniqueness, booking optimistic-lock version, relationships, and nullable release timestamp.
   - Keep API DTOs, JPA entities, and domain models distinct. Do not add business workflows or behavior for unresolved booking, cancellation, pricing, VAT, or invoice policies.

4. [x] **Create the initial Flyway schema**
   - Add a new versioned migration defining tables for customers, container types, containers, bookings, container assignments, invoices, and invoice lines, plus the approved storage for address fields.
   - Add UUID primary keys, foreign keys, unique constraints, non-null constraints, check constraints for approved numeric/status invariants, and indexes for foreign-key and documented query paths.
   - Encode the approved database-level protection against overlapping active assignments. Do not modify any existing migration; add a new migration if a correction is needed after application.
   - Confirm table names and column definitions match the approved domain/API mappings and do not create or alter Keycloak-owned tables.

5. [x] **Add persistence and migration tests**
   - Add PostgreSQL Testcontainers integration tests that start from an empty database, apply Flyway migrations, and exercise each repository's required save/read/update behavior and entity relationships.
   - Verify documented unique, foreign-key, check, and assignment-overlap constraints fail with PostgreSQL when violated.
   - Verify optimistic-lock behavior where it is part of the approved model and confirm nullable assignment release timestamps map correctly.
   - Verify migrations are applied once, report the expected schema version, and leave the Keycloak database untouched. Keep any Compose-backed test opt-in as the existing `COMPOSE_DATABASE_TEST` test is.
   - Run the backend test/build tasks and verify the existing generated OpenAPI sources still compile without manual edits.

6. [x] **Document persistence operations**
   - Document the migration location, local PostgreSQL startup, clean Testcontainers test commands, and how to add future schema changes as new migrations.
   - Update `docs/data-model.md` and, only if approved decisions change architecture guidance, `docs/architecture.md` to reflect the resolved model and database invariants.
   - Do not edit the roadmap or mark this plan complete until the approved implementation succeeds.

## Files and Components Affected

- `backend/build.gradle.kts` — JPA/Flyway/Kotlin JPA dependencies and compiler plugin.
- `backend/src/main/resources/application.yml` — Flyway/JPA configuration only if defaults do not meet the schema-validation requirements.
- `backend/src/main/resources/db/migration/V1__*.sql` — initial application schema migration.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/domain/**` — domain entities, value objects, and enums.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/**` — repository ports only where required by the selected architecture boundary.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/infrastructure/persistence/**` — JPA entities, Spring Data repositories, adapters, and mappers.
- `backend/src/test/kotlin/de/pipplip/abfallcontainerservice/**` — domain, mapping, repository, migration, and constraint tests.
- `docs/data-model.md` and possibly `docs/architecture.md` — approved model/schema clarifications.
- `README.md` — local migration and persistence test instructions.
- `planning/roadmap.md` and this plan — status updates only after approved implementation succeeds.

Generated code, Docker Compose services, Keycloak configuration, and local secret files were not changed. The OpenAPI specification was updated only for the approved numeric precision and scale.

## Required Database Migrations

- Add the first versioned Flyway migration for the application schema. It should define the approved tables and relationships for customers, container types, physical containers, bookings, container assignments, invoices, and invoice lines.
- Include UUID primary keys; foreign keys; required unique, not-null, check, and index definitions; booking version support; and PostgreSQL enforcement for non-overlapping active assignments as resolved in the open questions.
- Use database types and numeric precision/scale only after the monetary representation and model discrepancies are approved.
- Do not create a Flyway schema/history table or application objects in the Keycloak database; Flyway's configured datasource must be the application database.
- Never edit an applied migration. Any post-application schema correction must be a new migration.

## Required API Changes

The existing contract already represents monetary values as integer `*Cents`
fields and includes booking quantity. `openapi/api.yaml` was updated to reflect
the approved numeric precision for container volume and invoice-line quantity
and tax rate. No endpoints or generated sources were changed manually.

## Security Implications

- Store the Keycloak subject as an opaque, unique customer identifier for later identity mapping; do not accept it as a customer-controlled ownership value or integrate Keycloak authentication in this milestone.
- Do not store passwords, access tokens, refresh tokens, or Keycloak credentials in application tables.
- Keep database credentials environment-driven and do not include real credentials in tests, migrations, or documentation.
- Ensure database constraints preserve ownership relationships and do not permit dangling customer, booking, assignment, or invoice references. Authorization and customer ownership checks remain in the Security and customer-feature milestones.
- Keep the Keycloak schema isolated from the application schema, including when both databases share the local Compose PostgreSQL server.

## Testing Requirements

- Run the Gradle Wrapper build and test tasks with the configured JDK 25 toolchain.
- Use PostgreSQL 17 Testcontainers for schema, migration, mapping, repository, and constraint integration tests; do not rely on an in-memory database to validate PostgreSQL-specific behavior.
- Verify a clean database receives the migration, Hibernate validates the resulting schema, and a second application startup does not rerun or alter an already-applied migration.
- Test persistence mapping for all aggregates/entities and relationships, including nested address fields, decimal or integer-cent monetary values as approved, quantity, and nullable timestamps.
- Test database rejection of duplicate unique values, invalid statuses/numeric values, missing referenced rows, and overlapping assignments as documented and approved.
- Verify the existing OpenAPI validation/generation and compile path is unaffected; do not commit generated output.
- If testing against Docker Compose, use the existing opt-in convention and verify the application's Flyway history and schema are isolated from Keycloak's database.
- Do not include an actual Keycloak server in ordinary persistence tests; no identity-provider behavior is under test here.

## Acceptance Criteria

- Gradle builds with the existing pinned Kotlin, Spring Boot, Gradle, and JDK versions and includes the required JPA/Flyway support.
- Domain models, JPA models, API DTOs, and mappings have distinct responsibilities and reflect the approved data model.
- Flyway creates the application schema from an empty PostgreSQL 17 database and Hibernate validates, but does not generate, schema changes.
- The initial migration contains the approved UUID keys, relationships, constraints, indexes, and assignment-overlap protection.
- Repository integration tests verify persistence behavior and PostgreSQL-enforced invariants using Testcontainers.
- Customer profiles can be associated with an opaque unique Keycloak subject without a Keycloak dependency or security implementation.
- Application migrations cannot change Keycloak-owned tables and use the application's existing environment-configured PostgreSQL database.
- No endpoint behavior, JWT/security behavior, booking allocation workflow, cancellation/invoicing business policy, Docker service, or production secret is introduced.
- Documentation and schema reflect only approved answers to the listed open questions.

## Definition of Done

- All approved implementation steps and acceptance criteria are complete.
- Gradle build and relevant unit/integration tests pass, including PostgreSQL migration and constraint tests.
- A new empty PostgreSQL database migrates successfully; already-applied migrations remain unchanged.
- The existing Compose Postgres/Keycloak environment remains usable, with migration activity isolated to the application database.
- Documentation matches the actual dependency, migration, configuration, and test commands.
- `planning/roadmap.md` is updated to mark the Persistence items complete only after implementation and verification succeed.
- This plan was approved before implementation and is COMPLETED after the migration, persistence integration tests, API validation, and backend build succeeded.

## Out of Scope

- API/controller/delegate implementation, OpenAPI changes without separate approval, or manual edits to generated code.
- Keycloak realm/client/user provisioning, JWT Resource Server configuration, role mapping, and authorization/ownership checks.
- Booking creation/allocation workflows, availability calculations, double-booking transaction orchestration, state-transition services, or cancellation behavior.
- Pricing and delivery-fee calculation, VAT policy, invoice-generation timing/calculation, payment-provider integration, or invoice PDF generation.
- Angular/frontend work, new Docker Compose services, application Dockerfile, production deployment, CI/CD, monitoring, and email notifications.
- Changes to the already-completed Project Setup or OpenAPI plans and milestones.

## Resolved Decisions

- Store every monetary amount in PostgreSQL `BIGINT` minor units. Keep the existing API integer `*Cents` fields and persist the associated ISO 4217 currency code.
- Persist positive integer `Booking.quantity`.
- Enforce assignment overlap with a PostgreSQL trigger and serialize checks by locking the assigned container row. An assignment is active while `released_at` is null and its booking is not cancelled.
- Use `NUMERIC(8,2)` for positive container volume in cubic meters, `NUMERIC(12,3)` for positive invoice-line quantity, and `NUMERIC(5,4)` for tax rates represented as fractions from 0 through 1.
- Enforce at most one invoice per booking with a unique `Invoice.booking_id`.
- Enforce customer email uniqueness in addition to the unique opaque Keycloak subject.
- Treat booking date intervals as half-open `[deliveryDate, pickupDate)`: delivery is included and pickup is excluded.
- Define repository ports in `application.port.out`, with only `save` and `findById` methods; keep Spring Data interfaces and adapters in `infrastructure.persistence`.
