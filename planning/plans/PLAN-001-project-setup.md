# PLAN-001: Project Setup

## Objective

Establish a buildable Kotlin/Spring Boot backend and a repeatable local development environment using Gradle, PostgreSQL, Keycloak, and Docker Compose. This plan covers the first roadmap milestone only and creates a foundation for the later API, persistence, and security milestones.

## Status

COMPLETED

## Scope

- Create the backend Gradle Kotlin DSL project and wrapper using JDK 25.
- Configure Spring Boot, Kotlin, baseline application settings, and a minimal application-context test.
- Configure environment-based PostgreSQL connectivity for local development.
- Define PostgreSQL and Keycloak as local-development services in Docker Compose.
- Document how to configure and start the local environment without committing credentials.
- Keep Keycloak available as an identity provider, without implementing application authorization or customer identity flows.

## Technical Approach

- Use the Gradle Kotlin DSL and checked-in Gradle Wrapper. Pin Gradle 9.3.0, Kotlin 2.3.21, and Spring Boot 3.5.16; these selected versions support the JDK 25 toolchain and are verified by the build.
- Use Spring Boot with Kotlin and constructor injection conventions. Create only the minimal application entry point and configuration needed to build and start the service.
- Read database connection settings from environment variables, with local values supplied through an ignored `.env` file and a committed example file containing placeholders only.
- Use Docker Compose for PostgreSQL and Keycloak local infrastructure. Pin service image versions, persist PostgreSQL data in a named volume, and add health checks and service readiness dependencies where supported.
- Keep PostgreSQL and Keycloak configuration suitable for local development only. Do not commit credentials, production configuration, or a production-ready Keycloak realm.
- Run the backend from Gradle on the host for this milestone. Defer a backend container image and its Compose service until the Dockerfile/deployment milestone.
- Do not add domain tables or Flyway migrations in this milestone; schema and migration work belongs to the Persistence milestone.

## Implementation Steps

1. [x] **Create the Gradle/Kotlin project**
   - Add Gradle settings, build configuration, wrapper, and project properties.
   - Configure Kotlin/JVM and Spring Boot plugins and the JDK 25 toolchain.
   - Add only dependencies needed to start Spring Boot and run the initial test suite.
   - Verify the wrapper can compile and test an empty application.

2. [x] **Add the Spring Boot application skeleton**
   - Add the application entry point and minimal runtime configuration.
   - Configure an application name and environment-driven settings without adding API endpoints or business logic.
   - Add a context-load test and verify the application starts using the configured JDK.

3. [x] **Configure PostgreSQL connectivity**
   - Add the PostgreSQL driver and environment-based datasource configuration.
   - Define local database name, user, and password inputs without embedding real credentials.
   - Verify the application can start with PostgreSQL available; do not create application tables or migrations.

4. [x] **Add local PostgreSQL and Keycloak services**
   - Add a root Docker Compose configuration for PostgreSQL and Keycloak with pinned image versions, persistent database storage, health checks, and local-only port mappings.
   - Configure Keycloak for local development and provide admin credentials through environment variables rather than committed secrets.
   - Verify Compose configuration and service startup/readiness.

5. [x] **Document local setup and verify the workflow**
   - Add a safe example environment file and ignore the developer-specific environment file.
   - Document prerequisites, environment setup, Compose startup/shutdown, and Gradle commands in the README.
   - Verify the documented workflow from a clean checkout as far as the available environment permits.

## Files and Components Affected

- `backend/settings.gradle.kts`
- `backend/build.gradle.kts`
- `backend/gradle.properties`
- `backend/gradlew`, `backend/gradlew.bat`, and `backend/gradle/wrapper/*`
- `backend/src/main/kotlin/**` — minimal Spring Boot entry point
- `backend/src/main/resources/application.yml` — environment-driven application and datasource settings
- `backend/src/test/kotlin/**` — application-context test
- `backend/src/test/kotlin/**` — opt-in Compose PostgreSQL connectivity test
- `compose.yaml` — local PostgreSQL and Keycloak services
- `docker/postgres/init/01-create-keycloak-database.sh` — provision a separate local Keycloak database
- `.env.example` — placeholder local configuration only
- `.gitignore` — exclude local environment files and generated/build output as needed
- `README.md` — local development and startup instructions - do not change existing content, only add new instructions for this milestone

Exact Kotlin package and test paths should follow the repository naming convention established during implementation.

## Required Database Migrations

None. PostgreSQL is provisioned for local development, but application schema creation and Flyway migrations are deferred to the Persistence milestone.

## Required API Changes

None. No API endpoints or OpenAPI specification changes are part of Project Setup.

## Testing Requirements

- Run the Gradle Wrapper build and test tasks with JDK 25.
- Verify the Spring application context loads with the minimal configuration.
- Verify PostgreSQL connectivity against the Compose service, without requiring application tables.
- Verify the PostgreSQL driver and application datasource with an isolated Testcontainers database.
- Validate the Compose file and confirm PostgreSQL and Keycloak reach healthy/ready states.
- Confirm local credentials are read from environment configuration and no secrets are committed.
- Confirm the README commands and environment-file instructions match the delivered configuration.

## Acceptance Criteria

- The backend builds and tests through the checked-in Gradle Wrapper with JDK 25.
- Kotlin, Gradle, and Spring Boot versions are pinned and mutually compatible with the required JDK.
- The Spring Boot application starts with configuration supplied through environment variables.
- The application can connect to the local PostgreSQL service without requiring schema objects.
- Docker Compose starts PostgreSQL and Keycloak with documented local configuration and persistent PostgreSQL data.
- Local credentials are not committed; the example environment file contains placeholders only.
- The README provides sufficient instructions to configure, start, stop, and verify the local development environment.
- No business logic, API, database schema, or production deployment behavior is introduced.

## Definition of Done

- Every implementation step is independently verifiable and complete.
- The Gradle build, tests, application startup, database connectivity, and Compose service checks pass.
- Setup documentation reflects the actual configuration and commands.
- No generated build output, local environment file, or secret is tracked.
- The plan status is changed from DRAFT only after explicit user approval; implementation does not begin until approval.

## Out of Scope

- OpenAPI specification, generated API code, controllers, or REST endpoints.
- Domain models, repositories, database tables, Flyway migrations, or business rules.
- JWT Resource Server configuration, role mapping, method authorization, customer ownership, or frontend authentication.
- Keycloak realm/client/user provisioning and production identity configuration.
- Backend or frontend Dockerfiles, frontend service, production Compose configuration, CI/CD, monitoring stack, or deployment hardening.
- Booking, invoicing, email, or other application functionality.

## Open Questions with answers

- Should the development Keycloak service import a preconfigured realm/client for local use, or should realm setup wait for the Security milestone? The proposed scope defers realm/client provisioning. - Use the proposed scope to defer realm/client!
- Are there preferred PostgreSQL and Keycloak image versions, or should implementation select and pin supported versions at that time? Use postgres:17-alpine and keycloak:26.3.1. The user selected Spring Boot 3.5.x with JDK 25 after verifying Spring Boot 3.3 only supports Java through 23.
- Should local development use an ignored `.env` file, or is another environment-variable management convention preferred? Yes use an ignored `.env` file with a committed example file containing placeholders only.
- Is an application health endpoint required in this setup milestone, or should health/liveness/readiness endpoints remain with the later hardening work? No health endpoint is required for this milestone.
