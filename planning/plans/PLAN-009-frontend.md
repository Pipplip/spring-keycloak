# PLAN-009: Frontend

## Objective

Create a responsive Angular single-page application for customer and
administrator workflows. Integrate the existing generated OpenAPI client with
the Spring Boot API and authenticate through the configured Keycloak public
client using Authorization Code Flow with PKCE. Keep authorization and
customer ownership enforcement in the backend.

## Status

IN PROGRESS

## Prerequisites

- PLAN-001 Project Setup, PLAN-002 OpenAPI, PLAN-003 Persistence, PLAN-004
  Security, PLAN-005 Customer Features, PLAN-006 Admin Features, PLAN-007
  Invoicing, and PLAN-008 Hardening are COMPLETED.
- `openapi/api.yaml` defines the customer and administrator operations, and
  Gradle tasks generate and structurally validate the Angular TypeScript client
  under `frontend/build/generated/openapi/typescript-angular/`.
- The existing Keycloak realm defines the `abfallcontainer-frontend` public
  client, local redirect origin `http://localhost:4200`, PKCE configuration,
  and USER/ADMIN roles.
- Root Docker Compose provides PostgreSQL and Keycloak by default. An opt-in
  `app` profile adds local-development backend and frontend services; the
  Spring Boot service generates the Angular client before the frontend starts.
- `frontend/` has generated build output but no tracked Angular application,
  package manifest, or npm lockfile.

## Scope

- Create the Angular CLI application under `frontend/` using standalone
  components, strict TypeScript and template checking, lazy-loaded feature
  routes, Angular Material, and the project's documented frontend conventions.
- Implement responsive shared navigation, theme, semantic page structure,
  keyboard-operable controls, visible focus, and accessible status and error
  messaging, targeting WCAG 2.2 AA.
- Configure Keycloak Authorization Code Flow with PKCE using the existing
  public client. Provide login/logout and role-aware navigation for USER and
  ADMIN without embedding a secret or persisting bearer/refresh tokens in
  browser web storage.
- Integrate the generated Angular client from `openapi/api.yaml`; configure
  the API origin per environment and attach tokens only to requests to that
  configured backend origin.
- Implement customer screens for active container catalogue/detail,
  availability, booking creation and listing/detail, cancellation where
  allowed, and associated invoice retrieval.
- Implement administrator screens for booking listing/detail and status
  actions, container-type management, physical-container and assignment
  management, customer management, and invoice creation, retrieval, and status
  actions using existing API operations and statuses.
- Provide typed reactive forms, explicit validation, and loading, empty,
  success, and API error states. Display safe messages derived from RFC 7807
  Problem Details without exposing internal diagnostics.
- Add unit/component tests, frontend API/auth/navigation tests, accessibility-
  critical interaction coverage, and browser-level coverage for core customer
  and administrator workflows.
- Document reproducible frontend setup, test, build, and local integration
  steps in the README.

## Technical Approach

- Keep `openapi/api.yaml` as the API source of truth. Use Gradle's existing
  `generateAngularOpenApi` and `validateAngularOpenApiOutput` tasks; do not
  hand-edit generated files, duplicate endpoint definitions, or encode backend
  business rules in the UI. If an implementation gap requires a contract
  change, update and validate the OpenAPI specification first, and get approval
  for any requirement change.
- Create an npm-managed Angular CLI application with a committed lockfile.
  Pin a supported Node.js LTS release compatible with the selected Angular
  version and use `npm ci` for reproducible installs. Use Angular Material as
  the sole UI component library.
- Keep cross-cutting authentication, API configuration, and Problem Details
  translation under `core`; organize customer and administrator screens under
  lazy-loaded `features`; put reusable presentational elements and formatting
  under `shared`. Keep components focused on presentation and interaction;
  prefer local state/signals and RxJS rather than adding a global state library.
- Configure the Keycloak adapter as a public browser client using the existing
  realm configuration and PKCE. Keep token handling in the adapter's in-memory
  lifecycle. Add a narrowly scoped interceptor that only supplies the current
  access token to requests whose origin matches the configured API origin.
  Route guards control navigation only; the backend's Spring Security
  authorization, role checks, and customer ownership remain authoritative.
- Consume the existing generated Angular client and models for catalogue,
  availability, bookings, administration, and invoices. Keep application-
  specific API base URL configuration outside generated output.
- PostgreSQL remains private to the Spring Boot backend. Frontend workflows
  use only the HTTP API; this milestone requires no schema changes or Flyway
  migrations. Run the existing Gradle validation/generation tasks alongside
  frontend npm checks to ensure the generated client stays in sync.
- Reuse the current Spring Boot/Kotlin/JDK 25 backend, PostgreSQL 17 service,
  and imported Keycloak realm. Run PostgreSQL, Keycloak, and the backend using
  the existing local workflow or the opt-in Compose `app` profile, with
  Angular's development server on its configured local origin. Configure
  backend CORS only if required for this documented local browser integration,
  restricting allowed origins and methods to the actual need; do not weaken
  authorization or expose credentials.
- Keep Dockerfiles and production/containerized frontend deployment in the
  Deployment milestone. The approved Compose application services are
  local-development-only, opt-in via the `app` profile, and use bind mounts
  rather than production images.
- Do not add a payment workflow, create unsupported operations, or infer
  backend behavior beyond the documented requirements, business rules, and
  OpenAPI contract. Existing admin APIs manage invoice states; email
  notifications remain deferred to FEAT-001.

## Implementation Steps

1. [x] **Confirm frontend runtime and test tooling**
   - Select an Angular CLI/Angular Material version compatible with a supported
     pinned Node.js LTS release; record the exact versions in the project
     manifests/toolchain configuration.
   - Select the Angular unit/component test setup and a browser-level test
     runner for core workflows. Keep tooling limited to the agreed test scope.
   - Approved resolution: add backend and frontend local-development services
     under an opt-in Compose `app` profile; keep production containerization in
     Deployment.

2. [x] **Scaffold the Angular application and reproducible npm workflow**
   - Create the Angular CLI project under `frontend/` with standalone
     components, strict TypeScript/template checks, and lazy-loaded route
     structure.
   - Add Angular Material, a shared theme foundation, package scripts, pinned
     Node.js tooling, and a committed package lockfile.
   - Verify `npm ci`, Angular compilation, and the baseline test command.

3. [x] **Configure environment, API client, and error handling**
   - Configure development and production API origins without modifying
     generated client sources.
   - Wire the generated OpenAPI client into application dependency injection
     and verify all current generated services/models are consumable.
   - Add centralized translation of RFC 7807 Problem Details into
     user-safe messages and provide consistent request failure handling.
   - Add focused tests for API base URL setup, expected Problem Details, and
     unexpected/network errors.

4. [x] **Implement Keycloak authentication and protected navigation**
   - Configure the existing `abfallcontainer-frontend` public client for
     Authorization Code Flow with PKCE, login, logout, and adapter-managed
     in-memory token lifecycle.
   - Implement origin-restricted bearer-token attachment and unauthenticated
     and USER/ADMIN navigation guards.
   - Test login-state handling, token attachment only to the backend origin,
     role-aware navigation, logout, and denial/fallback for missing roles.
   - Verify the backend remains the authorization boundary and no customer ID
     or role supplied by the UI is treated as trusted.

5. [x] **Build accessible shared shell and customer workflows**
   - Implement responsive application navigation, page layout, accessible
     loading/empty/error/success feedback, and common status presentation.
   - Implement catalogue and container detail, availability, typed booking
     creation, own booking list/detail, cancellation for eligible bookings, and
     invoice retrieval using the generated client.
   - Cover validation, API errors, pagination where exposed by the contract,
     cancellation eligibility presentation, keyboard interactions, and
     customer navigation in focused tests.

6. [x] **Build administrator workflows**
   - Implement lazy-loaded ADMIN views for bookings, statuses, container types,
     physical containers and assignments, customers, and invoices using
     existing generated API methods.
   - Add typed forms and confirmation for consequential actions; display API
     validation, authorization, conflict, and not-found failures without
     duplicating server rules.
   - Cover USER/ADMIN navigation, representative management forms/actions,
     loading/empty/error states, and accessibility-critical interactions.

7. [ ] **Verify end-to-end browser integration**
   - Run the existing Docker Compose PostgreSQL and Keycloak services and start
     the backend through Gradle or the opt-in Compose `app` profile using the
     current local configuration.
   - Start the Angular application at the configured Keycloak redirect origin
     and validate public catalogue/availability plus authenticated USER and
     ADMIN workflows against the real API.
   - Verify CORS is limited to the required local frontend origin, Keycloak
     redirects work, protected routes return to the expected login flow, and
     no bearer token is sent to unrelated origins.
   - Run browser-level coverage for the core customer journey and a
     representative administrator journey; do not make test success depend
     silently on external credentials or services.
   - Playwright test discovery succeeds, but live browser execution is blocked
     in the current environment because Chromium's `libnspr4` dependency is
     unavailable. The Compose stack also cannot be started here without local
     `.env` values and test identities/inventory.

8. [ ] **Document and verify the frontend milestone**
   - Update README instructions with Node/npm prerequisites, reproducible
     frontend install/run/test/build commands, API and Keycloak configuration,
     and local service startup requirements. Preserve existing documentation
     and avoid documenting real secrets.
   - Run `npm ci`, frontend lint/check/build/test tasks, browser tests, and the
     backend Gradle OpenAPI validation/client generation checks.
   - Update the roadmap and plan status only after explicit approval,
     implementation, and verification succeed.

## Files and Components Affected

- `frontend/package.json`, `frontend/package-lock.json`, Angular CLI
  configuration, TypeScript configuration, and Node.js toolchain pin.
- `frontend/src/**` — application shell, routes, core auth/API/error handling,
  shared accessible components/theme, and customer/admin feature screens.
- `frontend` unit/component and browser-test configuration and test sources.
- `frontend` environment configuration — API base URL and public Keycloak
  settings only; no client secret or production secret.
- `backend/build.gradle.kts` — no change expected; it already defines Angular
  client generation and output validation tasks. Change only if an approved
  generator integration defect requires it.
- `backend/src/main/kotlin/**` — possibly a narrowly scoped CORS configuration
  adjustment if necessary for the configured local SPA origin; no business
  workflow or authorization change is expected.
- `docker/keycloak/realm/abfallcontainer-realm-realm.json` — no change expected;
  the existing public client already allows the local Angular origin.
- `compose.yaml` — opt-in, local-development backend and frontend services;
  production frontend/backend containerization is deferred to Deployment.
- `README.md` — frontend development, configuration, integration, and test
  instructions.
- `openapi/api.yaml` — no changes expected; edit only before generated clients
  if an approved API contract change is required.
- `planning/roadmap.md` and this plan — status updates after successful
  implementation and verification only.
- Generated Angular client output at
  `frontend/build/generated/openapi/typescript-angular/` is generated build
  output and must never be edited manually.

## Required Database Migrations

None. The frontend consumes the Spring Boot API and never connects directly to
PostgreSQL.

## Required API Changes

None expected. Existing customer and administration operations cover the
roadmap UI workflows. Any discovered missing behavior or contract mismatch
must be raised for approval and changed in `openapi/api.yaml` before client
regeneration; do not add frontend-only business rules or call undocumented
endpoints.

## Security Implications

- Use only the existing Keycloak public client and Authorization Code Flow
  with PKCE. Do not embed client credentials, collect passwords in the app, or
  persist bearer/refresh tokens in `localStorage` or `sessionStorage`.
- Add bearer tokens only to requests targeting the configured backend API
  origin. Validate this behavior with an unrelated-origin test.
- Treat frontend role guards as navigation UX only. Spring Security continues
  to enforce authorization, and the backend continues to derive customer
  ownership from the verified JWT subject.
- Never trust customer IDs, ownership, or authorization decisions supplied by
  browser state or request forms. Use only generated client operations and
  backend-authoritative responses.
- Render Problem Details as user-safe messages; do not surface stack traces,
  SQL errors, tokens, or internal exception details.
- If backend CORS changes are needed for local development, allow only the
  explicitly configured SPA origin and required API methods/headers; never
  enable broad origin patterns with credentials.
- Keep API, Keycloak, database, and admin credentials out of tracked frontend
  configuration and documentation. Frontend configuration may contain public
  issuer, realm, client ID, and API origin only.

## Testing Requirements

- Install dependencies reproducibly with `npm ci`; verify the pinned Node.js
  version and Angular toolchain compatibility.
- Run Angular type/template checks, lint, production build, and unit/component
  tests using the scripts established in the frontend package manifest.
- Test API configuration, generated-client integration, loading/empty/success
  and RFC 7807/network error states, form validation, and role-aware routes.
- Test authentication state, PKCE login/logout behavior through the configured
  adapter, origin-restricted token attachment, and protected navigation.
- Test keyboard operation, semantic labels/status feedback, visible focus,
  responsive layouts at supported viewport sizes, and other accessibility-
  critical interactions; include automated accessibility checks where
  compatible with the selected tooling.
- Run browser-level tests against the live local stack for a customer journey
  (catalogue through booking and own-booking view/cancel as appropriate) and a
  representative admin workflow. Assert server authorization remains enforced
  for protected API requests.
- Start PostgreSQL and Keycloak through root Docker Compose; run the backend
  with the JDK 25 Gradle wrapper or opt-in `app` profile, and confirm the SPA
  can call the API and authenticate against the existing realm. Use test
  identities/configuration without committing credentials.
- Run from `backend`: `.\gradlew.bat openApiValidate
  generateAngularOpenApi validateAngularOpenApiOutput` (or equivalent
  `./gradlew` commands under Bash) and confirm generated files are build output
  rather than hand-maintained frontend code.
- No database migration or database-specific frontend test is required.

## Acceptance Criteria

- A clean checkout can install, test, and build the Angular application using
  the pinned Node.js LTS and committed npm lockfile.
- The application uses standalone Angular components, strict type/template
  checks, lazy-loaded feature routes, and Angular Material as its only UI
  component library.
- USER workflows cover active catalogue, availability, booking creation and
  own booking reads, eligible cancellation, and associated invoice retrieval
  through the generated OpenAPI client.
- ADMIN workflows cover the existing booking, container type, container and
  assignment, customer, and invoice administration APIs.
- The UI presents clear validation and loading, empty, success, and safe error
  states, and does not implement business logic that conflicts with the
  backend contract.
- Keycloak login/logout uses the existing public client and PKCE. No client
  secret or browser web-storage token persistence is introduced.
- Authorization headers are attached only to the configured backend API
  origin. Frontend guards do not replace backend role enforcement or customer
  ownership checks.
- Customer and administrator navigation and core interactions work with
  keyboard input and responsive layouts, and accessibility checks are
  satisfied for the agreed WCAG 2.2 AA target.
- The application integrates locally with the existing Spring Boot API,
  PostgreSQL and Keycloak setup. PostgreSQL is never accessed directly from
  browser code, and no schema or Flyway changes are made.
- The generated Angular client remains reproducible from `openapi/api.yaml`
  through Gradle tasks and no generated file is manually modified.
- Relevant frontend checks, browser workflows, and OpenAPI generation/
  validation checks pass; README instructions match the delivered setup.
- The Deployment milestone's Dockerfiles, CI pipeline, and production
  containerization remain untouched; the approved local Compose app profile
  does not add production deployment behavior.

## Definition of Done

- This plan is explicitly approved before implementation starts.
- All in-scope customer/admin screens, authentication/API integration,
  accessibility behaviors, tests, and setup documentation are complete.
- The frontend builds and tests from a clean npm install, browser tests
  exercise the agreed local API workflows, and OpenAPI generation/validation
  succeeds through Gradle.
- Keycloak and API settings contain only public configuration; token handling,
  origin scoping, and backend authorization boundaries are verified.
- No direct database access, manual generated-source changes, unapproved API or
  business-rule changes, or accidental deployment scope has been introduced.
- `planning/roadmap.md` is updated only after implementation and verification
  succeed, and this plan is marked COMPLETED at that point.

## Out of Scope

- Changes to booking, inventory, customer, invoice, tax, or payment business
  rules; payment-provider integration and email notification implementation.
- New API endpoints or contract behavior not already represented in
  `openapi/api.yaml`.
- Direct browser-to-PostgreSQL access, schema changes, Flyway migrations, or
  changes to backend customer ownership and authorization policies.
- Keycloak confidential clients, secret distribution, realm redesign, or
  production identity configuration.
- Production Dockerfiles, multi-stage frontend/backend images, production
  Compose topology, CI/CD, publishing, Swagger UI, health-check, monitoring,
  or tracing work assigned to the Deployment milestone.
- Broad redesign of the Spring Boot/Kotlin architecture or unrelated backend
  refactoring.

## Open Questions

- Compose application services: **Resolved** — add frontend and backend
  services for local development under the opt-in `app` profile; keep the
  default Compose startup infrastructure-only.
- Browser-level test runner: **Resolved** — Playwright.
- VAT presentation: **Resolved** — display API-calculated invoice amounts
  without altering the VAT policy or implying confirmation.
