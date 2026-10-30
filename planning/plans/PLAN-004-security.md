# PLAN-004: Security

## Objective

Integrate Keycloak-issued JWT authentication with the Spring Boot API, enforce
the public, USER, and ADMIN access boundaries already described by the OpenAPI
contract, and establish a safe authenticated-customer identity/ownership
boundary without trusting client-supplied customer identifiers.

## Status

COMPLETED

## Prerequisites

- PLAN-001 Project Setup is COMPLETED.
- PLAN-002 OpenAPI is COMPLETED.
- PLAN-003 Persistence is COMPLETED.
- The project has a Keycloak 26.3.1 service in Docker Compose and a separate
  Keycloak database, but no configured realm, application clients, realm roles,
  or Spring Security integration.
- The OpenAPI contract already marks container-type catalogue and availability
  operations public, customer booking operations as bearer-authenticated USER
  operations, and `/api/v1/admin/**` operations as bearer-authenticated ADMIN
  operations.
- Resolve the decisions under Open Questions before implementation. This plan
  must not add undocumented profile-provisioning or token-validation policy.

## Scope

- Add Spring Security's OAuth2 Resource Server support to the Gradle backend.
- Configure stateless JWT authentication and issuer/signature/time validation
  against the approved Keycloak realm.
- Map the documented Keycloak realm roles `USER` and `ADMIN` to Spring Security
  authorities and enforce the OpenAPI route access matrix, denying unclassified
  API routes by default.
- Provide a trusted current-customer identity boundary based on the verified
  JWT `sub` claim, backed by the existing unique customer Keycloak subject.
- Add a booking ownership authorization component that compares the persisted
  booking owner with the authenticated customer; never accept an owner ID from
  request data as proof of ownership.
- Configure reproducible, non-secret local Keycloak realm/client/role setup in
  Docker Compose only after the open realm, client, issuer, and provisioning
  decisions are approved.
- Test JWT role mapping, API access boundaries, customer identity resolution,
  and ownership restrictions.
- Document local setup, required environment configuration, and test commands.

## Technical Approach

- Use Spring Security OAuth2 Resource Server rather than a custom JWT parser.
  Configure an environment-driven issuer URI so Spring validates JWT signatures,
  issuer, and time-based claims using Keycloak discovery/JWK metadata.
- Configure a stateless `SecurityFilterChain`. Permit only the documented
  catalogue and availability routes; require `ROLE_USER` for customer booking
  routes and `ROLE_ADMIN` for `/api/v1/admin/**`. Deny unclassified `/api/v1`
  routes by default, and do not infer authorization from generated OpenAPI
  annotations alone.
- Add a Keycloak-specific `JwtAuthenticationConverter` for the approved realm
  role claim representation. Do not map arbitrary token claims or treat an
  unrecognized role as an authenticated application role.
- Keep authentication infrastructure under the documented `security` or
  `config` package boundary. Keep customer identity resolution behind an
  application port/use-case boundary; infrastructure may read the authenticated
  Spring Security principal but domain code must not depend on Spring Security.
- Resolve the authenticated customer using the verified `sub` value and the
  existing unique `Customer.keycloakSubject`. Extend the existing repository
  port/adapter for lookup only if required. Do not create a customer implicitly
  from token claims until a profile-provisioning policy is approved.
- For booking ownership, compare the authenticated customer's persisted ID
  against the booking's persisted `customerId` before returning or mutating
  customer-owned data. Treat missing customers/bookings and non-ownership
  explicitly; do not silently fall back to request-supplied IDs.
- Preserve `openapi/api.yaml` as the authorization contract. Do not change
  generated sources or implement customer/admin endpoint behavior in this
  security milestone. Since route handlers are not implemented yet, verify
  filter decisions using Spring Security integration tests and exercise the
  ownership policy at its application boundary.
- Do not add a Flyway migration: customer subject uniqueness is already part of
  the persistence schema. Do not modify the application database or the
  separate Keycloak database for security state.
- Keep Compose credentials environment-driven and out of tracked realm exports.
  Configure local Keycloak import/startup and health readiness only after the
  approved realm and client decisions are known. Any local bootstrap accounts
  must use explicitly supplied environment values and must not be production
  accounts or secrets.
- Return unauthenticated and forbidden API responses using the existing
  RFC 7807 Problem Details convention without weakening the distinction
  between 401 and 403.

## Implementation Steps

1. [x] **Approve Keycloak and token decisions**
   - Resolve the realm name, frontend/backend client identifiers, role
     representation, and local issuer/hostname strategy.
   - Decide whether audience validation is required and which audience(s) are
     valid.
   - Decide how customer profiles are provisioned and what happens when an
     authenticated USER has no local profile.
   - Confirm where booking ownership checks are invoked while customer booking
     handlers/use cases are not yet implemented.

2. [x] **Add OAuth2 Resource Server dependencies and configuration**
   - Add Spring Security and OAuth2 Resource Server dependencies using the
     existing Spring Boot dependency platform.
   - Add environment-driven issuer configuration for local host execution and
     Compose execution, ensuring the configured issuer exactly matches the
     token's `iss` claim and is reachable from the backend runtime.
   - Keep PostgreSQL, Flyway, and Keycloak database settings isolated and
     unchanged; do not add secrets to application configuration.

3. [x] **Implement JWT role mapping and route authorization**
   - Add a stateless `SecurityFilterChain` and Keycloak realm-role converter for
     the approved claim shape.
   - Implement the documented public/USER/ADMIN route matrix and a deny-by-
     default rule for other API routes.
   - Configure explicit RFC 7807 authentication-entry-point and access-denied
     responses with correct 401 and 403 semantics.
   - Ensure generated OpenAPI classes remain generated build output and are not
     manually edited.

4. [x] **Resolve authenticated customer identity and ownership**
   - Add a current-customer application boundary that obtains the verified
     subject from the authenticated principal and resolves the local customer
     profile.
   - Extend the customer repository port and persistence adapter with a
     `keycloakSubject` lookup if needed; rely on the existing unique database
     constraint and add no schema migration.
   - Add an ownership authorization component for bookings that checks
     `Booking.customerId` against the resolved customer ID.
   - Make missing profile, missing booking, and non-owner behavior explicit
     according to the approved API/error policy; do not add implicit customer
     creation.

5. [x] **Configure local Keycloak in Docker Compose**
   - Add the approved realm/client/role import or bootstrap configuration using
     Keycloak 26.3.1-supported mechanisms.
   - Configure only the approved development redirect origins and client access
     type/flow; use Authorization Code Flow with PKCE for the Angular public
     client as required by the architecture.
   - Add service readiness/health configuration only as necessary for reliable
     startup and keep Keycloak on its existing separate database.
   - Do not commit credentials, access tokens, refresh tokens, or production
     realm exports.

6. [x] **Add security and ownership tests**
   - Add focused unit tests for role claim mapping, including missing,
     malformed, unknown, and multiple-role cases.
   - Add Spring Security integration tests proving public routes pass without
     credentials, protected routes return 401 without a valid token, USER and
     ADMIN access the correct route groups, cross-role access returns 403, and
     unclassified API routes are denied.
   - Test issuer/signature rejection and the approved audience behavior using
     a controlled decoder/JWK setup; avoid relying on external Keycloak in the
     ordinary test suite.
   - Test current-customer lookup by verified subject and booking ownership
     for owner, non-owner, missing profile, and missing booking cases.
   - Use PostgreSQL Testcontainers where ownership tests exercise repository
     behavior. Keep any real-Keycloak/Compose smoke test opt-in and separate
     from ordinary Gradle tests.
   - Confirm generated Kotlin OpenAPI sources still compile and existing
     persistence tests remain green.

7. [x] **Document and close the milestone**
   - Document local Keycloak startup, approved realm/client/role setup,
     required issuer/audience environment variables, and security test
     commands.
   - Update architecture/requirements documentation only where an approved
     security decision changes or clarifies it.
   - Mark the Security roadmap items and this plan complete only after the
     approved implementation and tests succeed.

## Files and Components Affected

- `backend/build.gradle.kts` — Spring Security and OAuth2 Resource Server
  dependencies.
- `backend/src/main/resources/application.yml` — environment-driven issuer and
  any approved JWT audience settings.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/security/**` —
  filter chain, JWT role conversion, current-principal access, and authorization
  support.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/**` —
  current-customer and booking-ownership boundaries where required by the
  selected clean-architecture design.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/port/out/PersistenceRepositories.kt`
  and `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/infrastructure/persistence/PersistenceAdapters.kt`
  — customer lookup by Keycloak subject if required.
- `backend/src/test/kotlin/de/pipplip/abfallcontainerservice/**` — JWT,
  authorization, customer identity, and ownership tests.
- `compose.yaml` and a new non-secret Keycloak realm configuration under
  `docker/keycloak/**` — only if the approved local provisioning approach uses
  Compose import/bootstrap.
- `README.md` and possibly `docs/architecture.md` — approved local setup and
  security behavior.
- `planning/roadmap.md` and this plan — status updates after successful
  implementation only.

No generated OpenAPI source, frontend implementation, Keycloak production
configuration, or Flyway migration is expected to change.

## Required Database Migrations

None expected. The existing customer table stores a unique `keycloak_subject`,
and the security milestone should use that existing identity reference. Add a
migration only if implementation uncovers a requirement that cannot be met
using the approved existing schema; obtain approval and create a new Flyway
migration rather than modifying `V1__create_persistence_schema.sql`.

No security-related tables or objects may be created in the separate Keycloak
database by application Flyway.

## Required API Changes

None expected. `openapi/api.yaml` already declares bearer authentication and
public, USER, and ADMIN operation boundaries. Runtime enforcement must match
that contract. Any discovered mismatch must be raised for approval before
changing the API specification or generated output.

## Security Implications

- Trust only JWTs whose signature, issuer, and time claims validate against the
  approved Keycloak realm; apply audience checks only as explicitly approved.
- Map only the approved Keycloak role claim to `ROLE_USER` and `ROLE_ADMIN`.
  Never grant access based on an unverified header, query parameter, request
  body, or arbitrary token claim.
- Derive customer identity only from the verified JWT `sub` and the local
  customer profile. Never use a client-supplied customer ID as ownership proof.
- Apply ownership checks to every customer-facing read or mutation of bookings
  and associated invoices when those handlers/use cases are implemented.
- Keep the service stateless and do not persist bearer tokens, refresh tokens,
  or Keycloak credentials.
- Keep Keycloak realm bootstrap credentials out of tracked files. Use local
  environment configuration and ensure any development users are not
  production identities.
- Preserve isolation between the application PostgreSQL database and the
  Keycloak database. Do not run Flyway against Keycloak's database.
- Return 401 for missing/invalid authentication and 403 for authenticated
  principals lacking the required role or ownership permission.

## Testing Requirements

- Use JUnit 5, MockK, Spring Security test support, and PostgreSQL 17
  Testcontainers where appropriate.
- Verify public, USER, ADMIN, and deny-by-default route behavior with valid and
  invalid authentication states.
- Verify Keycloak realm roles are converted to only the approved application
  authorities and malformed/absent roles never grant access.
- Verify JWT issuer, signature, expiration, and approved audience validation.
- Verify identity resolution uses only the authenticated subject and rejects
  unresolved/absent customer profiles according to the approved policy.
- Verify a customer cannot access another customer's booking or invoice, and
  that request-supplied customer identifiers cannot override the authenticated
  owner.
- Verify Problem Details responses preserve the API's 401/403 semantics.
- Run backend unit and integration tests, the OpenAPI validation/generation
  compile path, and the Gradle build using the configured JDK 25 toolchain.
- If an opt-in Compose/Keycloak test is added, verify Keycloak readiness,
  issuer/JWKS reachability from the backend runtime, role claims, and
  application-vs-Keycloak database isolation without requiring production
  secrets.

## Acceptance Criteria

- Valid JWTs from the approved Keycloak issuer authenticate through Spring
  OAuth2 Resource Server validation; invalid signatures, issuer, timestamps,
  and any approved audience requirement are rejected.
- Only the documented public routes are accessible without authentication.
- USER and ADMIN authorities map from the approved Keycloak role claims and
  enforce the access matrix in `openapi/api.yaml`; unmatched API routes fail
  closed.
- 401, 403, and ownership-denied responses are explicit and conform to the
  approved RFC 7807 error behavior.
- The authenticated customer is resolved from verified `sub` using the
  existing unique local subject reference; no client-supplied owner ID can
  change the identity.
- Booking ownership authorization distinguishes the owner from a non-owner and
  is covered by tests. Customer booking handlers/use cases remain responsible
  for invoking this boundary when implemented.
- Local Keycloak development setup is reproducible with approved
  realm/client/role choices and no committed credentials or tokens.
- No migration, manual generated-source edit, endpoint business behavior,
  production Keycloak setup, or implicit profile provisioning is introduced
  without explicit approval.

## Definition of Done

- All approved implementation steps and acceptance criteria are complete.
- Gradle build, backend unit/integration tests, OpenAPI validation/generation
  compile path, and PostgreSQL-backed ownership tests pass.
- Security behavior matches the OpenAPI contract and does not rely on
  client-controlled identity data.
- The local Compose/Keycloak path works using only documented environment
  configuration and does not expose secrets or alter the Keycloak database
  with application migrations.
- Documentation reflects the implemented issuer, role, customer lookup, and
  local setup decisions.
- `planning/roadmap.md` is updated after all approved implementation and
  verification steps succeed; this plan is COMPLETED only after all
  acceptance criteria are verified.

## Out of Scope

- Implementing generated API delegates/controllers or customer/admin business
  workflows.
- Customer self-registration, account linking, profile creation/synchronization,
  or automatic user/customer provisioning until separately approved.
- Angular authentication UI, browser token storage, login/logout flows, or
  client-side role guards.
- Production realm administration, production client secrets, password grant,
  or production Keycloak deployment.
- Booking allocation, cancellation eligibility, pricing, invoicing, or any
  business rule not already documented.
- Flyway schema changes, changes to Keycloak-owned database objects, and
  changes to the OpenAPI contract unless separately approved.
- Full application Dockerfile, CI/CD, deployment, monitoring, or unrelated
  roadmap milestones.

## Resolved Decisions

- Import a version-controlled local realm named `abfallcontainer-realm` with
  clients `abfallcontainer-frontend` and `abfallcontainer-backend`, and realm
  roles `USER` and `ADMIN`.
- Allow only `http://localhost:4200/*` as the local frontend redirect URI and
  `http://localhost:4200` as its web origin. Use Authorization Code Flow with
  PKCE (S256); do not enable implicit flow or direct access grants.
- Use `http://localhost:8081/realms/abfallcontainer-realm` as the canonical
  issuer for both host and container execution. A backend container may use
  `http://keycloak:8080/realms/abfallcontainer-realm/protocol/openid-connect/certs`
  for JWKS retrieval while validating the canonical issuer.
- Require the `abfallcontainer-backend` audience and map only `USER` and
  `ADMIN` from Keycloak's `realm_access.roles` claim.
- Provide an opt-in Compose bootstrap service for one local application
  administrator. Supply its username and password separately through
  environment variables; do not commit credentials or provision production
  users.
- Deny authenticated USER requests without a local customer profile with 403.
  Do not auto-provision profiles from JWT claims; customer-management work may
  define profile creation separately.
- Implement and test a reusable booking ownership boundary in this milestone.
  Future booking handlers/use cases must invoke it before returning or mutating
  customer-owned data.

## Open Questions

None. All plan decisions were approved before implementation.
