
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

Local Keycloak security configuration:
- Realm: `abfallcontainer-realm`; application realm roles are `USER` and `ADMIN`.
- The Angular public client is `abfallcontainer-frontend` and uses Authorization
  Code Flow with PKCE (S256). Its local redirect origin is
  `http://localhost:4200`.
- The API audience is `abfallcontainer-backend`. The backend accepts only this
  audience and maps only the Keycloak realm role claim to USER/ADMIN authorities.
- The canonical local token issuer is
  `http://localhost:8081/realms/abfallcontainer-realm`. A backend container may
  use the internal Keycloak JWKS endpoint while retaining this issuer for token
  validation.
- Customer ownership is derived from the verified JWT `sub` and the local
  customer's unique Keycloak subject. Authenticated USER tokens without a local
  customer profile are denied; profiles are not provisioned automatically.

Frontend:
- Angular SPA.
- Public Keycloak client `abfallcontainer-frontend`.
- Authorization Code Flow with PKCE (S256).
- Do not embed a client secret or persist access/refresh tokens in
  `localStorage` or `sessionStorage`.
- UI role guards improve navigation only. The API remains responsible for all
  authorization and customer ownership decisions.

## 6. Frontend Application

- Create a conventional Angular CLI application under `frontend/`; use
  standalone components, strict TypeScript/template checking, and lazy-loaded
  feature routes. Keep the application deliberately small: avoid NgRx or other
  global state frameworks until a demonstrated need exists; use Angular
  signals for local view state and RxJS for asynchronous API operations.
- Use npm with a committed lockfile for reproducible installs; pin a supported
  Node.js LTS version compatible with the selected Angular CLI in the
  frontend toolchain configuration and use `npm ci` in CI.
- Use Angular Material as the single component library for accessible forms,
  navigation, tables, dialogs, and status indicators. Apply a small shared
  theme and responsive layout rather than introducing a second design system
  or a custom component framework.
- Organize code by responsibility: `core` for authentication, API
  configuration, and app-wide error handling; `features` for customer and
  administrator routes; and `shared` for reusable presentational components
  and formatting utilities. Keep feature components focused on rendering and
  user interaction.
- Use the generated TypeScript Angular client from `openapi/api.yaml` for
  backend calls and API models. Generate it with the existing Gradle task;
  never edit generated files by hand. Keep generated output outside
  hand-maintained source files and configure the API base URL per environment.
- Centralize authenticated API access in the HTTP client/interceptor
  integration. Attach a Keycloak access token only to requests to the
  configured backend API origin; do not leak bearer tokens to unrelated
  origins. Keep API calls out of templates and avoid duplicating endpoint
  definitions or server-side business rules in the browser.
- Configure OIDC through the existing Keycloak public client with Authorization
  Code Flow + PKCE. Keep tokens in memory under the OIDC adapter's management,
  use the adapter's refresh lifecycle, and send users through the normal
  authorization flow rather than collecting or handling passwords in the app.
- Use route guards to direct unauthenticated users to login and to hide
  irrelevant USER/ADMIN navigation. These checks are only UX; backend
  authorization and ownership checks remain mandatory.
- Provide customer routes for the catalogue, availability, booking creation,
  own bookings, and booking invoice retrieval. Provide an administrator area
  for booking, container type, container, customer, and invoice management.
  Reuse existing API operations and statuses; do not introduce frontend-only
  business behavior.
- Use typed reactive forms for data entry, inline field validation, confirmation
  for consequential actions, and explicit loading/empty/error/success states.
  Translate RFC 7807 Problem Details into clear user messages while keeping
  diagnostic details out of the UI.
- Build semantic, responsive pages with keyboard support, visible focus,
  labels, adequate contrast, and accessible status text. Prefer shared
  responsive navigation, simple forms, cards, and data tables over a
  dashboard-heavy design. Target WCAG 2.2 AA.
- Test feature components and services with Angular's testing utilities,
  including API error/loading states, auth integration, and role-based
  navigation. Add browser-level coverage for the core customer and
  administrator flows when the frontend milestone is implemented.

## 7. Database Architecture

- PostgreSQL.
- Spring Data JPA.
- Flyway migrations.
- Foreign keys and constraints.
- Transactions for booking allocation.
- Optimistic locking where appropriate.
- Prevent overlapping container assignments.

## 8. Email Integration

The backend sends emails for:
- Booking confirmation.
- Booking cancellation.

Invoice lifecycle notifications are deferred to FEAT-001 in the planning
backlog and are not sent by the invoicing milestone.

The email provider is still to be selected.

## 9. Docker and Deployment

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

## 10. Health and Monitoring

Use Spring Boot Actuator.

Provide:
- Health checks.
- Liveness.
- Readiness.

## 11. Future CI/CD

- Build.
- Unit tests.
- Integration tests.
- Docker image build.
- Docker image publishing.
