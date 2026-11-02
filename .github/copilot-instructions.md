# Project Instructions

## Project
Abfallcontainer-Service

A web application for renting waste containers.

## Technology Stack
- Backend: Kotlin and Spring Boot, JDK 25
- Frontend: Angular
- Database: PostgreSQL
- Database Migration: Flyway
- Build Tool: Gradle (Kotlin DSL)
- Architecture: Clean Architecture and Domain-Driven Design
- Authentication: Keycloak (OpenID Connect)
- API Design: OpenAPI 3.x + openapi-generator
- Testing: JUnit 5, MockK, Testcontainers
- Deployment: Multi-Stage-Dockerfile and docker-compose
- Monitoring/Tracing: Grafana and OpenTelemetry

## Architecture and Coding Guidelines
- Follow Clean Architecture principles.
- Apply Domain-Driven Design where appropriate.
- Follow SOLID principles.
- Use dependency injection and constructor injection.
- Keep controllers thin.
- Business logic belongs in the application and domain layers.
- Separate API DTOs from JPA entities.
- Use explicit mappers between API and domain models.
- Keep responsibilities clearly separated.
- Avoid unnecessary abstractions.

## Kotlin Coding Standards
- Follow official Kotlin coding conventions.
- Prefer immutable data structures.
- Use null safety appropriately.
- Use meaningful names.
- Write readable and maintainable code.

## Security
- Keycloak handles authentication.
- The backend acts as an OAuth2 Resource Server.
- Use JWT authentication.
- Enforce role-based authorization.
- Enforce customer ownership checks.
- Never trust customer IDs supplied by clients.
- Never expose credentials, tokens or secrets.
- Test authorization and ownership restrictions.

## Persistence
- Use PostgreSQL and Spring Data JPA.
- All database changes must use Flyway migrations.
- Never modify an already applied migration.
- Use transactions for booking operations.
- Prevent double bookings.
- Apply appropriate database constraints and indexes.

## API
- Follow API-first development.
- OpenAPI is the single source of truth.
- API version prefix: /api/v1.
- Generate API DTOs and interfaces using openapi-generator.
- Never manually modify generated sources.
- Use RFC 7807 Problem Details for errors.
- Validate incoming requests.
- Keep API models separate from persistence entities.

## Angular Frontend
- Build the browser application with Angular standalone components, strict
  TypeScript/template checking, and lazy-loaded feature routes.
- Use npm with a committed lockfile and a supported, pinned Node.js LTS
  version; use `npm ci` for reproducible installs.
- Use Angular Material as the single UI component library; keep layouts
  responsive, semantic, keyboard accessible, and visually consistent, with
  WCAG 2.2 AA as the accessibility target.
- Use the generated TypeScript Angular client for backend calls. Never edit
  generated sources or duplicate API endpoints and server business rules.
- Keep components focused on presentation; centralize authentication,
  API-client configuration, and cross-cutting error handling.
- Use typed reactive forms for user input, with explicit validation and
  loading, empty, success, and error states.
- Use Keycloak Authorization Code Flow with PKCE as a public client. Never
  embed client secrets or store bearer/refresh tokens in browser web storage.
- Attach bearer tokens only to requests for the configured backend API origin.
  Frontend role guards are navigation UX only; the backend enforces
  authorization and customer ownership.
- Prefer local component state/Angular signals and RxJS over adding global
  state-management libraries without a demonstrated need.
- Test components, API behavior, accessibility-critical interactions, and
  USER/ADMIN navigation.

## Testing
- Use JUnit 5 and MockK.
- Use Testcontainers for integration tests.
- Write unit tests for business logic.
- Write integration tests for persistence and APIs.
- Test security and ownership restrictions.
- Use Angular's test tooling for frontend unit/component tests and cover core
  browser workflows with end-to-end tests.
- Run relevant tests after implementation.

## Documentation
- Read relevant documentation before implementation.
- Update documentation when requirements or architecture change.
- Update planning files after implementation.
- Never silently change approved decisions.

## Agent Workflow
1. Analyze before implementing.
2. Read relevant project documentation.
3. Create implementation plans for development tasks.
4. Store plans in planning/plans/.
5. Use sequential IDs (PLAN-001, PLAN-002, etc.).
6. Each plan must contain acceptance criteria and a Definition of Done.
7. Never implement a newly created plan automatically.
8. Wait for explicit user approval.
9. Implement only the approved scope.
10. Run relevant tests.
11. Review the code for correctness, security, and maintainability.
12. Update plan status and roadmap after successful implementation.
13. Report changed files and test results.
