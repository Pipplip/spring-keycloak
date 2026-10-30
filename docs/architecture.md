
# Architecture

## 1. Technology Stack

| Area | Technology |
|---|---|
| Backend Language | Kotlin |
| JDK | 25 |
| Build | Gradle Kotlin DSL |
| Backend | Spring Boot |
| API | Spring Web MVC |
| API Specification | OpenAPI 3.x |
| API Generator | openapi-generator |
| Authentication | Keycloak |
| Security | Spring Security |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| Migration | Flyway |
| Tests | JUnit 5, MockK |
| Frontend | Angular |
| Monitoring and Tracing | Grafana and OpenTelemetry |


## 2. Architecture Style

Clean Architecture with domain-driven design with separated layers.

## 3. Backend Structure

- api: Controllers, delegates and exception handling.
- application: Business use cases and services.
- domain: Domain entities, enums and business rules.
- infrastructure: Persistence and external integrations.
- mapper: API DTO and domain mapping.
- security: JWT and authorization.
- config: Application configuration.

## 4. API-First Development

OpenAPI is the single source of truth.

Specification:
openapi/api.yaml

Generation:
- Generator: kotlin-spring for backend, typescript-angular for frontend
- interfaceOnly: true
- useSpringBoot3: true
- useTags: true
- delegatePattern: true
- useBeanValidation: true

Generated sources:
build/generated

Generated DTOs are API models only.
JPA entities must remain separate.

The OpenAPI specification is initially
created by GitHub Copilot based on the
requirements, data model and business rules.

All subsequent API changes must first
be reflected in openapi/api.yaml.

## 5. Security Architecture

Keycloak handles authentication.

Backend:
- OAuth2 Resource Server.
- JWT validation.
- Realm role mapping.
- USER and ADMIN roles.
- SecurityFilterChain.
- Method-level authorization.
- Customer ownership validation.

Frontend:
- Angular SPA.
- Public Keycloak client.
- Authorization Code Flow with PKCE.

## 6. Database Architecture

- PostgreSQL.
- Spring Data JPA.
- Flyway migrations.
- Foreign keys and constraints.
- Transactions for booking allocation.
- Optimistic locking where appropriate.
- Prevent overlapping container assignments.

## 7. Email Integration

The backend sends emails for:
- Booking confirmation.
- Booking cancellation.

The email provider is still to be selected.

## 8. Docker and Deployment

- Multi-stage Dockerfile.
- JDK 25 build environment.
- Minimal JRE runtime.
- Non-root container user.
- Docker Compose for local development.

Services:
- backend
- frontend
- postgres
- keycloak
- optional pgadmin
- grafana

Configuration through environment variables (yaml files)
and Spring profiles.

Production secrets must never be committed.

## 9. Health and Monitoring

Use Spring Boot Actuator.

Provide:
- Health checks.
- Liveness.
- Readiness.

## 10. Future CI/CD

- Build.
- Unit tests.
- Integration tests.
- Docker image build.
- Docker image publishing.
