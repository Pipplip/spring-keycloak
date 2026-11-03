# PLAN-010: Deployment

## Objective

Package the Kotlin/Spring Boot API and Angular application as deployable
containers, provide a production-oriented Docker Compose topology for the
application with PostgreSQL and Keycloak, add health checks and a CI validation
pipeline, and document the resulting deployment workflow. Preserve the existing
local-development Compose workflow and the established API, data, and security
contracts.

## Status

IN PROGRESS

## Progress

- Implemented and locally verified the backend and frontend images, health
  endpoints, local-only Swagger UI, image-based Compose deployment, CI workflow
  configuration, and deployment documentation.
- Backend and frontend checks passed; both deployment images built, Compose
  profiles validated, and the deployment stack passed health, API, database
  isolation, and persistence smoke checks.
- The user approved proceeding while PLAN-009 remained IN PROGRESS after
  confirming the frontend production build and Keycloak configuration were
  ready for deployment packaging.
- Remaining verification: interactive Keycloak browser login and authenticated
  USER/ADMIN browser workflows, plus execution of the CI workflow on GitHub.

## Prerequisites

- PLAN-001 through PLAN-009 are COMPLETED.
- PLAN-009 (Frontend) is listed as complete in the roadmap, but its plan still
  has status IN PROGRESS. Confirm the frontend milestone is complete before
  implementing deployment packaging that depends on its production build.
- `openapi/api.yaml` remains the API source of truth. Gradle generates the
  Kotlin/Spring interfaces and Angular client; generated files are build
  outputs and must not be edited manually.
- The current root `compose.yaml` is for local development: PostgreSQL and
  Keycloak start by default, while the `app` profile runs Spring Boot with
  `bootRun` and Angular's development server using source mounts.
- The repository has no application Dockerfiles, `.dockerignore` files, or CI
  workflow. Spring Boot Actuator is not currently a backend dependency.
- PostgreSQL 17 and Keycloak 26.3.1 are already configured in Compose.
  PostgreSQL initializes a separate database for Keycloak; Flyway targets only
  the application datasource.

## Scope

- Add a multi-stage backend Dockerfile that builds the Spring Boot executable
  using the Gradle Wrapper, Kotlin, and JDK 25, then runs it on a minimal
  compatible Java 25 runtime image as a non-root user.
- Add a multi-stage frontend Dockerfile that installs dependencies
  reproducibly with the pinned Node/npm toolchain, builds Angular for
  production, and serves the static application from a small web server image.
- Add an image-based Compose deployment topology for the backend, frontend,
  PostgreSQL, and Keycloak, while retaining the existing local-development
  services and `app` profile behavior.
- Configure application health, liveness, and readiness checks and wire
  Compose health dependencies where they improve startup correctness.
- Add a Swagger UI entry point backed by the existing `openapi/api.yaml`
  contract; do not create a second or runtime-inferred API contract.
- Add a CI pipeline that validates and tests the backend and frontend and
  builds both container images. Image publishing and automatic deployment are
  not included unless explicitly approved.
- Update the README and non-secret example configuration for image builds,
  Compose deployment, health verification, service URLs, and safe shutdown.

## Technical Approach

- Preserve Gradle Kotlin DSL, Gradle Wrapper, Kotlin 2.3.21, Spring Boot
  3.5.16, and the JDK 25 toolchain. Build the backend through existing Gradle
  tasks, including OpenAPI validation/generation as required by compilation,
  tests, and `bootJar`; do not install an unrelated build system in the image.
- Use a multi-stage backend image: a JDK 25 build stage and a compatible
  Java 25 runtime stage. Copy only the executable artifact into the runtime
  image, run as a dedicated non-root user, and keep build caches and source
  files out of the final image. Add build-context exclusions without excluding
  required Gradle wrapper, OpenAPI, or application inputs.
- Build the Angular app with `npm ci` using the pinned Node.js 22.23.4 and npm
  10.9.9 toolchain, then serve only production static assets in the final
  frontend image. Do not include Node development tooling or `node_modules` in
  the final runtime image.
- Keep browser configuration public and secret-free. The deployed frontend
  must use the configured public API and Keycloak issuer URLs; select a
  configuration mechanism that does not require baking deployment-specific
  secrets into the image. Keep the Keycloak client public with PKCE and attach
  bearer tokens only to the configured API origin.
- Add Spring Boot Actuator only for the required health groups. Expose only
  health information required for orchestration, avoid including credentials,
  connection details, or other sensitive diagnostics, and ensure the
  unauthenticated health endpoint does not weaken API authorization. Distinguish
  process liveness from dependency readiness; PostgreSQL availability may
  affect readiness but must not cause a liveness restart loop.
- Use Compose deployment services that run built images rather than
  `bootRun`, `ng serve`, or source bind mounts. Preserve separate PostgreSQL
  application and Keycloak databases, persistent database storage, Flyway's
  application-database target, and the existing local-only `app` development
  workflow. Use non-secret configuration variables and ignored local secret
  files; do not add real credentials to tracked files.
- Run Keycloak in an appropriate non-development mode for the deployment
  topology, with deployment-specific public hostname/issuer and frontend
  redirect configuration. Do not use the development bootstrap credentials
  or localhost realm URLs as production defaults. Keep TLS termination and
  public network exposure decisions explicit rather than implying Compose
  provides those protections.
- Implement Swagger UI from the existing OpenAPI file and document its URL.
  Keep the contract as `openapi/api.yaml`, and do not expose interactive
  administrative capabilities more broadly than the approved deployment
  policy.
- Implement CI as validation, not release automation: run the existing
  Gradle validation/test/build tasks, reproducible Angular lint/test/build
  checks, and Docker image builds. Use a provider and trigger model approved
  for the repository. Do not publish images, deploy, or require production
  credentials in CI without explicit approval.
- Do not add a database migration or business API change for this milestone.
  Any implementation-discovered change to schema, API semantics, Keycloak
  policy, or public deployment behavior must be treated as an open decision
  and approved before implementation.

## Implementation Steps

1. [x] **Confirm deployment and CI decisions**
   - Resolve the open questions about target environment, network/TLS
     boundary, deployment URLs, image configuration, CI provider, and
     Swagger UI access.
   - Confirm PLAN-009 is complete and the frontend production build and
     Keycloak configuration are ready for deployment packaging.
   - Record the approved Compose profile/service naming and secret-injection
     approach without changing approved application architecture.

2. [x] **Package and verify the backend image**
   - Add a multi-stage Dockerfile and appropriate build-context exclusions.
   - Build the application with the checked-in Gradle Wrapper and JDK 25;
     run the generated-client validation and backend tests needed for the
     image build.
   - Run the packaged application with a Java 25 runtime as a non-root user,
     using environment-based PostgreSQL and Keycloak configuration.
   - Verify the image starts, applies only the application Flyway migrations,
     and serves the existing API without changing authorization behavior.

3. [x] **Add health and readiness endpoints**
   - Add the minimal Actuator configuration for liveness, readiness, and
     health checks.
   - Restrict exposed health details and integrate the endpoints with Spring
     Security without relaxing authentication for application API routes.
   - Add tests for endpoint status, dependency readiness, access policy, and
     safe response content.

4. [x] **Package the frontend image**
   - Add a multi-stage Node 22.23.4 build and static-server runtime image.
   - Configure SPA route fallback and the approved public API/Keycloak runtime
     or build configuration without secrets or a rebuild requirement that
     conflicts with the approved deployment strategy.
   - Verify the built UI loads, deep links resolve, and API/auth requests use
     only the configured origins.

5. [x] **Add Swagger UI for the API contract**
   - Serve Swagger UI backed by `openapi/api.yaml` at the approved URL and
     access boundary.
   - Verify the specification is current, valid, and not duplicated by
     generated or inferred runtime descriptions.

6. [ ] **Complete the image-based Docker Compose topology**
   - Add deployment services for the backend and frontend images alongside
     PostgreSQL and Keycloak, preserving the existing local development
     services and profile behavior.
   - Configure database persistence, separate application and Keycloak
     databases, environment variables, health checks, and healthy-service
     dependencies. Avoid source bind mounts and development servers in the
     deployment topology.
   - Configure Keycloak's deployment mode, hostname, realm/client redirect
     values, and backend issuer/JWKS settings according to the approved
     external URL and network topology.
   - Verify clean startup, health, API access, persistence across container
     recreation, and graceful shutdown using placeholder/local credentials
     only. Interactive Keycloak browser login and authenticated browser
     workflows remain unverified.

7. [ ] **Add continuous integration**
   - Add the approved CI-provider workflow for Gradle/OpenAPI validation and
     backend tests/build, plus Angular lint/tests/build.
   - Build both container images and validate the Compose deployment
     configuration without publishing images or deploying.
   - Keep CI independent of production secrets. The workflow is configured and
     its constituent checks passed locally, but execution and failure
     reporting on GitHub remain unverified.

8. [x] **Document deployment and record milestone progress**
   - Update README deployment prerequisites, configuration, build, start,
     health verification, Swagger UI, network/security boundaries, and
     shutdown instructions.
   - Update `.env.example` only with safe placeholders and required variable
     names; never commit local or production credentials.
   - Run the locally available Gradle, npm, Docker build, Compose
     configuration, and deployment smoke checks; update the roadmap and plan
     to reflect completed work and remaining verification without closing the
     milestone prematurely.

## Files and Components Affected

- `backend/Dockerfile` — multi-stage Spring Boot build/runtime image.
- `backend/.dockerignore` or root build-context exclusions — exclude
  generated/build artifacts, local configuration, and secrets while retaining
  all required build inputs.
- `frontend/Dockerfile` and frontend static-server configuration — Angular
  production build, SPA fallback, and approved public runtime configuration.
- `compose.yaml` — image-based deployment topology, health checks, and
  startup dependencies while preserving local development behavior.
- `backend/build.gradle.kts` — Actuator dependency and any minimal packaging
  configuration.
- `backend/src/main/resources/application.yml` — health groups and safe
  deployment configuration.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/security/SecurityConfiguration.kt`
  — narrowly scoped health endpoint access, if required by the selected
  Actuator policy.
- `backend/src/test/kotlin/de/pipplip/abfallcontainerservice/**` — health
  endpoint and security coverage.
- `openapi/api.yaml` — source document for Swagger UI; no contract change is
  expected.
- `.github/workflows/**` or the approved CI-provider configuration — backend,
  frontend, Compose, and image-build checks.
- `.dockerignore` files and `.gitignore` — exclude local credentials, build
  outputs, and other unnecessary context from images.
- `.env.example` — non-secret deployment configuration placeholders, if
  needed.
- `README.md` — deployment and verification instructions.
- `planning/roadmap.md` and this plan — deployment milestone status after
  implementation is verified.

Generated Kotlin and Angular sources remain build outputs and must not be
manually modified.

## Required Database Migrations

None expected. The deployment topology must retain PostgreSQL persistence and
Flyway's current application-database behavior. Do not alter any applied
migration; if a schema change becomes necessary, stop and obtain approval for a
separate requirement and a new migration.

## Required API Changes

No business API changes are expected. The existing OpenAPI contract remains
the single source for Swagger UI. Spring Boot Actuator health endpoints are
operational endpoints rather than additions to the business API contract;
their URL, exposure, and access policy must be explicit and tested.

## Security Implications

- Never place secrets in Docker build arguments, image layers, frontend assets,
  tracked environment files, CI logs, or health responses. Use the approved
  runtime secret mechanism and document required variables without values.
- Keep the browser Keycloak client public and PKCE-enabled. Use environment-
  appropriate redirects and issuers, restrict CORS to the approved frontend
  origin, and retain backend JWT audience, role, and customer-ownership
  enforcement.
- Keep PostgreSQL off public interfaces unless explicitly required and
  approved. Maintain the separate Keycloak database and ensure the application
  backend/Flyway never connects to it.
- Run backend and frontend containers with least privilege; the backend must
  run as non-root. Minimize image contents and ensure secrets and local
  configuration are excluded from build contexts.
- Do not use Keycloak development mode for deployment. Configure its public
  hostname and proxy/TLS behavior to match the approved ingress environment.
- Expose only the minimal health information needed for orchestration. Health
  checks must not reveal credentials, database URLs, token data, stack traces,
  or internal service details.
- Treat Compose as a deployment packaging mechanism, not as a substitute for
  TLS, ingress controls, secret management, backups, monitoring, or
  high-availability design.

## Testing Requirements

- Backend: run Gradle OpenAPI validation/generation as required, `test`,
  `build`/`bootJar`, and tests covering health/readiness responses and their
  security policy.
- Frontend: run `npm ci`, lint, tests, and production build with the pinned
  Node/npm versions; verify the static image serves assets and SPA deep links
  and uses the configured public API and Keycloak URLs.
- Images: build both multi-stage images; inspect runtime behavior to verify
  non-root backend execution, absence of local secrets/build tooling in final
  images, and successful startup with injected configuration.
- Compose: validate the deployment configuration; start the complete stack,
  wait for healthy/readiness states, verify API and frontend access and
  Keycloak login/redirect flow, check database persistence, and confirm
  PostgreSQL and Keycloak remain isolated.
- Swagger UI: verify it loads the existing `openapi/api.yaml`, renders the
  documented endpoints, and follows the approved access policy.
- CI: verify all intended checks run on the approved triggers, failures fail
  the workflow, and no production secret, registry push, or deployment is
  required.
- Regression: run existing backend, frontend, persistence, security, and
  relevant browser tests to confirm deployment work does not change application
  behavior.

## Acceptance Criteria

- The backend image builds from the repository using the Gradle Wrapper,
  Kotlin, Spring Boot, and JDK 25, and runs on a compatible Java 25 runtime as
  a non-root user.
- The frontend image is built reproducibly with the pinned Node/npm versions
  and serves the Angular production build without a development server or
  Node toolchain in the runtime image.
- Deployment configuration supports public API and Keycloak URLs without
  embedding secrets in frontend assets or images.
- Compose can run the image-based backend and frontend with PostgreSQL and
  Keycloak, retains database persistence and separate Keycloak/application
  databases, and does not regress the local-development profile.
- Liveness/readiness checks are accurate, safe, and usable by Compose; API
  authorization remains enforced outside the explicitly approved health
  endpoint.
- Swagger UI displays the existing OpenAPI source and has an explicit,
  approved access boundary.
- CI validates and tests backend and frontend code, builds the two images, and
  validates Compose without publishing or deploying unless separately
  approved.
- README and example configuration accurately describe the verified workflow
  and contain no real credentials.
- No applied database migration is changed; generated sources and business API
  behavior remain unchanged.

## Definition of Done

- All approved implementation steps and acceptance criteria are complete.
- Gradle/OpenAPI, backend and frontend tests/builds, Docker image builds,
  Compose configuration, and the approved deployment smoke checks pass.
- The backend is least-privileged, health endpoints disclose no sensitive
  details, and existing JWT, role, ownership, and CORS boundaries remain
  intact.
- PostgreSQL persistence and application/Keycloak database isolation are
  demonstrated; local development instructions continue to work.
- Swagger UI reflects `openapi/api.yaml`; CI behavior and deployment
  instructions are documented and verified.
- No secrets are committed or copied into image layers; no migration is
  modified; the roadmap and plan are updated only after verification.

## Out of Scope

- Kubernetes, cloud-specific infrastructure, high availability, autoscaling,
  ingress/certificate provisioning, production backup/restore operations, or
  a full observability stack.
- Automated image publishing, release versioning, production deployment,
  rollback automation, or registry credentials unless explicitly approved.
- Changes to booking, inventory, customer, invoicing, payment, or other
  business behavior; API contract redesign; or schema changes.
- Changing Keycloak roles, audience policy, client type, token policy, or
  customer provisioning behavior.
- Replacing Docker Compose as the deployment platform or restructuring the
  application architecture.

## Open Questions

- What is the intended deployment target and exposure model: a single host
  using Compose, or a development/test-only Compose deployment with a separate
  production platform? The current repository does not specify a hosting
  provider, domain names, ingress, or TLS termination. - The plan is to start the docker compose and use locally
- Which external URLs must be used for the frontend, API, and Keycloak, and
  where does TLS terminate? These values affect CORS, OIDC issuer/redirect
  configuration, Keycloak hostname/proxy settings, and service exposure. - Use the localhost for the frontend and backend, and use the internal docker network for the keycloak
- Should one immutable frontend image support multiple environments through
  non-secret runtime configuration, or is an environment-specific image build
  acceptable? The current README describes production URLs in
  `environment.prod.ts`, which are build-time values. - Use environment variables to configure the frontend and backend, and use the same image for all environments
- Which CI provider and triggers should be used? GitHub Actions is a possible
  fit, but the repository currently has no workflow configuration. - use GitHub Actions and trigger on push to main and merge requests. The actions should run the tests and build the images, but not publish them to a registry.
- Should CI only build images, or also publish them to a registry? If
  publishing is desired, which registry, tagging policy, and credentials
  mechanism are approved? - Only build the images, do not publish them to a registry. The images should be built and used locally for testing and deployment.
- Where should Swagger UI be hosted (backend image, frontend image, or a
  separate service), and should it be available publicly, only to operators,
  or only in local/development environments? - only local/development environments, and it should be hosted in the backend image
- What runtime mechanism should supply production secrets: Docker Compose
  secrets, an external secret manager, or another approved mechanism? A
  committed `.env.example` can document names but cannot safely provision
  production values. - Use a `.env` file that is not committed to the repository, and use Docker Compose secrets to inject the values into the containers. The `.env.example` file should contain placeholder values and variable names, but not real credentials.
- PLAN-009 is marked complete in the roadmap but IN PROGRESS in its plan.
  Should Deployment implementation wait until PLAN-009 is formally closed? - check if the frontend build is complete and the Keycloak configuration is ready for deployment packaging. If so, then proceed with the deployment plan. If not, then wait until PLAN-009 is formally closed before proceeding with the deployment plan.
