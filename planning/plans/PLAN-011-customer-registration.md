# PLAN-011: Customer Registration and Profiles

## Objective

Enable customers to create an account through the approved Keycloak
registration flow, complete and maintain their local customer profile, and use
that profile with existing customer features. Keep credentials and identity
authentication in Keycloak, derive profile ownership from the verified JWT,
and preserve the existing admin customer-management API.

## Status

COMPLETED

## Prerequisites and Dependencies

- PLAN-001 through PLAN-008 are marked COMPLETED.
- PLAN-009 remains IN PROGRESS pending authenticated customer/administrator
  browser verification and execution of the CI workflow on GitHub. This plan
  preserves that status and does not claim those broader checks.
- PLAN-010 remains IN PROGRESS. Reuse its existing Docker/Compose deployment
  configuration and verify the imported Keycloak realm in the environments
  this feature supports; do not make deployment completion a prerequisite for
  local feature implementation unless the approved deployment design requires
  it.
- The project already has a Keycloak realm, a public Angular client with
  Authorization Code Flow + PKCE, a USER role, JWT subject extraction, a
  local `Customer` profile, customer persistence, and administrator customer
  list/read/update operations.
- `Customer.keycloakSubject` and `Customer.email` are unique in the existing
  PostgreSQL schema. Authenticated USERs without a local profile are
  intentionally denied access to existing customer booking routes.
- Resolve the decisions under Open Questions before implementing registration
  or changing the profile/API contract.

## Scope

- Provide a customer-facing path to register an identity with Keycloak without
  collecting or handling passwords in the Angular application or Spring API.
- Create a local customer profile for an authenticated customer using the
  verified Keycloak subject, with the required billing address and existing
  profile fields.
- Allow customers to read and update only their own profile, while preserving
  the existing administrator customer-management operations.
- Keep Keycloak as the identity and credential authority and PostgreSQL as the
  local customer-profile store. Do not trust a subject, customer ID, or role
  supplied in a request body.
- Update the OpenAPI contract before generating Kotlin and Angular client
  output if new self-service operations are approved.
- Add backend, PostgreSQL, security, frontend, and opt-in Keycloak/Compose
  verification for registration, profile creation, ownership, and profile
  updates.
- Update relevant requirements, architecture, and local setup documentation
  only for decisions approved for this feature.

## Technical Approach

- **Registration identity:** The recommended approach is Keycloak-hosted
  self-registration through the existing public client. The browser redirects
  through the normal OIDC Authorization Code Flow with PKCE and never receives
  a client secret or handles the user's password directly. Enabling
  self-registration, assigning the USER role to new accounts, and requiring
  email verification remain subject to the Open Questions.
- **Local profile onboarding:** After authentication, the Angular app detects
  the absence of a local profile and presents the approved profile-completion
  flow. The backend obtains the subject only through the existing verified
  `AuthenticatedSubjectProvider`; a profile-create request must not accept a
  `keycloakSubject`, `customerId`, or role. The exact source and synchronization
  policy for email and names must be approved before mapping token claims or
  accepting those values from profile input.
- **Self-service API:** The current contract has only ADMIN customer
  list/read/update operations. Add narrowly scoped self-service endpoints only
  after their operations, request/response semantics, and error behavior are
  approved. A possible shape is `GET /api/v1/customers/me` and
  `PUT /api/v1/customers/me`, with a profile-creation operation that permits
  an authenticated USER who has no local profile yet. Reuse existing address
  and profile data concepts where appropriate, but do not expose admin
  customer IDs or return the Keycloak subject.
- **Authorization and ownership:** Allow the approved profile-onboarding
  operation only to an authenticated USER and only for the current subject;
  it must work before a local profile exists. Profile read/update operations
  require a resolvable local profile and cannot target another customer.
  Update the Spring Security route policy explicitly without weakening the
  existing default-deny API policy or the booking ownership checks. Preserve
  RFC 7807 401/403/404/409 behavior as approved.
- **Application and persistence:** Keep delegates/controllers thin. Add or
  extend an application service for profile onboarding and current-customer
  profile read/update, reusing `CustomerRepository`, `CurrentCustomerService`,
  existing domain `Customer` and API/domain mapping conventions. Translate
  subject/email uniqueness races to an explicit conflict and let unrelated
  persistence failures surface normally.
- **Database:** The existing `customers` table already stores the needed
  profile fields and enforces unique email and subject. No migration is
  expected if the approved behavior fits the current model. If an approved
  requirement adds persisted fields or account/profile state, add a new Flyway
  migration; never modify an applied migration or migrate the separate
  Keycloak database.
- **OpenAPI and generated clients:** Keep `openapi/api.yaml` authoritative.
  Update and validate it before implementing new routes, then use Gradle's
  existing Kotlin and Angular generation tasks. Never edit generated output
  manually.
- **Frontend:** Add a visible registration entry point that starts the
  Keycloak-hosted registration/login flow and a focused profile
  completion/edit experience using the generated Angular client. Keep forms
  typed and accessible with explicit validation and loading, success, and
  error states. Frontend guards remain navigation UX; backend authorization
  remains authoritative.
- **Keycloak and Compose:** If approved, update the existing non-secret
  Keycloak realm import for self-registration and the approved default-role
  and verification settings. Continue using the existing Compose Keycloak
  service, separate Keycloak database, and health/readiness setup; no new
  Compose service or credential is expected. Verify whether the deployment
  Compose overlay consumes the same realm import.
- **Build and dependencies:** Reuse the pinned Gradle, Kotlin, Spring Boot,
  JDK 25, PostgreSQL, and OpenAPI Generator setup. The hosted Keycloak flow
  needs no backend Keycloak Admin API client or additional runtime dependency.
  Reassess dependencies only if an explicitly approved alternative requires
  server-side Keycloak user administration.

## Implementation Steps

1. [x] **Approve account-registration and profile policies**
   - Decide whether registration is self-service and whether it uses the
     Keycloak-hosted UI or a backend-mediated account-creation flow.
   - Decide the default role for new accounts, any approval/activation policy,
     and whether verified email is required before profile creation or use.
   - Decide which profile fields are collected during onboarding and which
     identity attributes are authoritative in Keycloak.
   - Decide the self-profile API operations, behavior for duplicate email or
     an already-provisioned subject, and whether profile/account deletion is
     part of this feature.
   - Confirm how the existing profile-less USER denial policy changes for the
     onboarding operation, and whether stale FEAT-001 notification references
     should be retargeted to FEAT-002 in related planning documentation.

2. [x] **Define and validate the self-service API contract**
   - Update `openapi/api.yaml` with only the approved registration/profile
     operations, schemas, validation constraints, security requirements,
     and RFC 7807/duplicate-conflict responses.
   - Ensure customer profile operations identify the current user implicitly
     and cannot accept a customer ID, Keycloak subject, or role as ownership
     input.
   - Run OpenAPI validation and generate the Kotlin and Angular clients using
     the existing Gradle tasks; do not hand-edit generated files.

3. [x] **Implement backend profile onboarding and self-service**
   - Add the approved use cases and delegate methods for profile creation,
     current-profile retrieval, and current-profile updates.
   - Resolve the authenticated subject exclusively from the verified JWT
     boundary. Apply the approved email/name source and normalization policy.
   - Keep admin profile operations intact and ensure the self-service path
     cannot alter identity, role, or customer ownership fields.
   - Map known unique-constraint conflicts to the contract's explicit response
     and preserve existing error handling for unexpected failures.

4. [x] **Enforce onboarding and profile authorization**
   - Permit only the approved authenticated USER onboarding route when no
     local profile exists; do not permit existing booking operations for
     profile-less users.
   - Require the current local profile for read/update operations and prevent
     cross-customer access.
   - Add security tests for anonymous, USER, ADMIN, missing-profile,
     existing-profile, and unauthorized access cases.

5. [x] **Configure Keycloak registration and local Compose behavior**
   - Apply only the approved self-registration, default USER role, email
     verification, and profile-attribute settings to the non-secret realm
     import.
   - Keep the existing public client and PKCE settings. Do not enable
     implicit/direct password grants or put credentials in the frontend,
     realm export, source, or committed environment files.
   - Verify Compose imports the settings and retains isolation between the
     application and Keycloak databases. Add no new service unless an approved
     decision makes one necessary.

6. [x] **Add profile and persistence tests**
   - Unit-test subject-derived ownership, profile validation, duplicate
     handling, and profile update behavior.
   - Use PostgreSQL Testcontainers to verify create/read/update behavior,
     unique-subject/email constraints, and transactional behavior. Confirm no
     migration is needed, or test a new additive migration if the approved
     model requires one.
   - Test that request fields cannot select a different owner or alter the
     Keycloak subject and that admin profile operations remain compatible.
   - Keep real-Keycloak and Compose smoke tests opt-in; ordinary Gradle tests
     must not depend on a running external Keycloak service.

7. [x] **Implement the Angular registration and profile experience**
   - Add registration/login navigation into the configured Keycloak flow and
     profile onboarding when an authenticated USER lacks a local profile.
   - Add a profile view/edit form for the approved fields using the generated
     client; do not duplicate API paths or server-side rules.
   - Cover validation, accessibility-critical interactions, API errors,
     loading/success states, profile-less onboarding, and successful return to
     customer workflows.

8. [x] **Document, verify, and close the feature**
   - Document approved registration behavior, profile ownership, local
     Keycloak setup, and relevant backend/frontend test commands.
   - Correct only approved, directly related documentation; do not implement
     or decide FEAT-002 email notifications as part of this work.
   - Verified with `./gradlew test bootJar validateAngularOpenApiOutput
     --no-daemon`; all backend tests passed.
   - Verified Angular tests (17), production build, and frontend lint.
   - Verified an isolated local Compose stack and the opt-in Playwright
     registration/profile workflow against the imported realm; the registration
     test passed and the full E2E run passed its three applicable tests (two
     authenticated booking/admin tests were skipped without test accounts).
   - Update backlog/roadmap and mark this plan complete only after explicit
     implementation approval and successful verification.

## Files and Components Affected

- `openapi/api.yaml` — approved customer self-profile operations and schemas.
- `backend/build.gradle.kts` — no dependency change expected for hosted
  registration; use existing OpenAPI generation and validation tasks.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/api/**` —
  generated-delegate implementation for approved self-service operations.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/service/**`
  — profile onboarding/read/update use cases.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/application/port/out/**`
  — only additional persistence operations required by the approved use cases.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/security/SecurityConfiguration.kt`
  — explicit access policy for onboarding and current-profile routes.
- `backend/src/main/kotlin/de/pipplip/abfallcontainerservice/infrastructure/persistence/**`
  — customer persistence behavior only if existing repository methods are
  insufficient.
- `backend/src/main/resources/db/migration/V*__*.sql` — only if an approved
  profile requirement needs a schema extension; never modify applied files.
- `backend/src/test/kotlin/de/pipplip/abfallcontainerservice/**` — unit,
  PostgreSQL integration, API, and security tests.
- `docker/keycloak/realm/abfallcontainer-realm-realm.json` — approved
  non-secret registration/default-role/verification settings.
- `compose.yaml` and `compose.deployment.yaml` — only if required to keep
  local/deployment realm configuration consistent; no additional service is
  expected.
- `frontend/src/app/app.routes.ts` and `frontend/src/core/auth/**` — approved
  registration and profile route/auth flow wiring.
- `frontend/src/features/customer/**` — profile completion and profile
  management UI.
- `frontend/src/**/*.spec.ts` — frontend registration/profile tests.
- `docs/requirements.md`, `docs/architecture.md`, `docs/business-rules.md`,
  `README.md`, and planning references — approved requirement and setup
  clarifications only.
- Generated Kotlin/Angular sources under build output — regenerate only; never
  edit manually.

## Required Database Migrations

No migration is expected for the current profile fields: `customers` already
stores a unique Keycloak subject and email, names, optional company name,
billing address, and timestamps. If an approved decision adds fields such as
verification or activation state, create a new Flyway migration with
appropriate constraints and tests. Do not alter V1 or any other applied
migration, and keep Flyway isolated from the Keycloak database.

## Required API Changes

The existing OpenAPI contract includes only ADMIN customer list, get, and
update operations; it has no customer registration or self-service profile
routes. Add approved self-service operations to `openapi/api.yaml` before
implementing delegates or regenerating clients. The exact route and
create/read/update semantics remain open. No password, Keycloak subject,
customer ID, or role may be accepted as user-controlled ownership data.

## Security Implications

- Keycloak remains responsible for credentials, authentication, and account
  identity. Never accept passwords in the Angular app or backend API and never
  embed a client secret in the public frontend.
- Resolve ownership from the validated JWT `sub`; do not trust profile request
  IDs or subject values. Do not expose the Keycloak subject in customer
  responses.
- Scope onboarding to the approved authenticated customer role and permit it
  without an existing profile only on the explicit onboarding operation.
  Existing booking routes must remain unavailable until profile creation has
  succeeded.
- Keep USER/ADMIN assignment under the approved Keycloak policy; public
  registration must not enable self-assignment of ADMIN or any arbitrary role.
- Protect customer email, names, and billing address as personal data. Return
  only the user's own profile on self-service routes and preserve the existing
  backend authorization boundary.
- Keep local Keycloak and database credentials in ignored local configuration
  or secret files. Do not commit credentials, tokens, or production realm
  exports.

## Testing Requirements

- Run backend unit tests for profile creation/update validation, subject
  derivation, duplicate handling, and explicit error mapping.
- Use PostgreSQL Testcontainers for customer persistence, unique subject/email
  conflicts, and any approved migration. Do not rely on an in-memory database
  for PostgreSQL constraints.
- Test API security: unauthenticated requests receive 401; only the approved
  USER onboarding flow works without an existing profile; profile reads and
  updates are current-customer-only; ADMIN cannot be assigned by customer
  input; existing booking and admin authorization continues to pass.
- Validate the OpenAPI contract, generate Kotlin and Angular clients, compile
  generated Kotlin, and run the relevant backend tests.
- Test Angular registration entry/navigation and profile completion/edit,
  including validation, success, API conflict/error, loading, and accessibility
  states. Verify profile-less users can finish onboarding and then access
  customer workflows.
- Use an opt-in Keycloak/Compose smoke test to verify realm import,
  self-registration, approved role and verification behavior, OIDC login,
  local profile creation, and separation of the application and Keycloak
  databases. Ordinary tests must not require external Keycloak.
- Run the existing frontend test/build commands and relevant CI checks.

## Acceptance Criteria

- A customer can create an identity using the approved Keycloak registration
  flow and authenticate with the public client using Authorization Code Flow
  with PKCE.
- A newly authenticated customer can complete a local profile with the
  approved required fields, and the backend associates it with the verified
  subject without trusting a client-supplied owner or identity.
- A customer can retrieve and update only their own approved profile fields.
  Existing administrator customer management remains functional.
- Existing booking operations remain inaccessible to authenticated users who
  have not completed profile onboarding, and become available after successful
  profile creation under the existing USER policy.
- Duplicate subject/email behavior is explicit, consistent with the API
  contract, and protected by PostgreSQL constraints.
- Credentials remain in Keycloak; the public frontend has no client secret and
  no password or token is persisted in browser web storage.
- The Keycloak realm and Docker Compose workflow support the approved local
  flow without exposing secrets or changing the separate Keycloak database.
- OpenAPI-generated clients, backend and frontend tests, and approved
  integration checks pass. No generated source is manually edited and no
  applied migration is changed.
- Documentation and planning status match only approved, verified behavior.

## Definition of Done

- All open decisions needed for implementation are explicitly approved and
  recorded in the relevant documentation.
- Every implementation step and acceptance criterion is complete and verified.
- The OpenAPI specification, backend authorization/application/persistence
  behavior, Keycloak configuration, Angular experience, and documentation
  agree on the same registration and profile lifecycle.
- Relevant Gradle, PostgreSQL Testcontainers, OpenAPI generation/validation,
  frontend, and approved Keycloak/Compose checks pass.
- No credentials, tokens, generated-source edits, unintended schema changes,
  or unapproved business rules are introduced.
- The backlog/roadmap and this plan are updated after implementation approval
  and successful verification. PLAN-009 and PLAN-010 retain their separate
  IN PROGRESS statuses.

## Out of Scope

- Booking, inventory, invoicing, payments, and administrator customer
  management changes beyond preserving compatibility with existing APIs.
- Email notification delivery, including booking and invoice lifecycle
  notifications assigned to FEAT-002 by the current backlog.
- Password storage, password reset implementation, or browser-side password
  collection; these remain Keycloak responsibilities.
- Account deletion, suspension, or administrative identity lifecycle
  management unless explicitly approved in the Open Questions.
- A Keycloak Admin API integration or confidential client unless the user
  explicitly approves backend-mediated user creation.
- Changes to completed architecture decisions, unrelated security policies,
  unrelated migrations, or production identity-provider configuration.

## Open Questions

- Should customers self-register through Keycloak's hosted registration UI,
  or should a trusted backend workflow create Keycloak users? Hosted
  registration is recommended to keep passwords and privileged Keycloak
  credentials out of the application. - Use the recommended Keycloak-hosted registration flow
- Should new accounts receive USER automatically, require an administrator
  approval, or follow another onboarding policy? Customer registration must
  never grant ADMIN. - new accounts should receive USER automatically, with optional email verification before profile creation
- Is Keycloak email verification required before profile creation or access to
  customer features? If required, which email-verified claim/state is trusted? - Email verification is not required before profile creation.
- Which identity attributes are authoritative in Keycloak (email, first name,
  last name), which profile fields are entered in the service, and how are
  later Keycloak attribute changes reflected in PostgreSQL? - The Keycloak email is authoritative for the profile email, and first/last names are authoritative for the profile names. Profile fields entered in the service will be stored in PostgreSQL, and any changes to these attributes in Keycloak will not automatically update the local profile; synchronization must be handled through a separate process if needed.
- Which self-profile operations and route shape are approved? Should profile
  creation be an explicit POST followed by GET/PUT, or should the first
  authenticated profile submission use another defined lifecycle? - The best practice is to have an explicit POST operation for profile creation, followed by GET and PUT operations for retrieving and updating the profile. This allows for clear separation of concerns and aligns with RESTful principles.
- How should an already-provisioned Keycloak subject or email collision be
  handled, including an account whose subject differs from the subject already
  associated with that email? - If a Keycloak subject or email collision occurs, the system should return a clear and explicit error response indicating the conflict. The error should specify whether the conflict is due to an existing subject or email, and provide guidance for resolving the issue, such as contacting support or using a different email address. The system should not allow the creation of a new profile if it would result in a duplicate subject or email in the database.
- Are customer profile deletion, account deactivation, or changes to
  Keycloak identities part of FEAT-001, or are profiles retained and editable
  only? - Yes, customer profile deletion and account deactivation are out of scope for FEAT-001. Profiles should be retained and editable only, with no functionality for deleting or deactivating accounts as part of this feature.
- The current backlog assigns notifications to FEAT-002, but some existing
  docs and plans still refer to invoice notifications under FEAT-001. Should
  those stale cross-references be corrected as documentation-only cleanup
  during this feature, without implementing notifications? - Yes, the stale cross-references to invoice notifications under FEAT-001 should be corrected as documentation-only cleanup during this feature. This ensures clarity and accuracy in the documentation without implementing any notification functionality, which remains part of FEAT-002.
- PLAN-009 is marked complete in the roadmap but IN PROGRESS in its plan, and
  PLAN-010 remains IN PROGRESS. Which planning status should be reconciled
  before implementation or deployment verification? - The planning status of PLAN-009 should be reconciled to reflect its completion in the roadmap, and any dependencies on PLAN-010 should be clearly identified. Implementation and deployment verification should proceed only after confirming that the statuses of both plans are accurately represented and any necessary prerequisites are met.
