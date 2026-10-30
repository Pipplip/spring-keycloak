# PLAN-002: OpenAPI

## Objective

Define the initial REST API as the single source of truth in `openapi/api.yaml`, then configure reproducible Kotlin/Spring and Angular client generation through Gradle. This plan covers the OpenAPI roadmap milestone only; it defines contracts and generated interfaces without implementing API behavior.

## Status

COMPLETED

## Prerequisites

- PLAN-001 Project Setup is COMPLETED.
- The backend Gradle project, Kotlin, Spring Boot, and JDK 25 toolchain are available.
- PostgreSQL, Keycloak, and Docker Compose are configured for local development. They are not required to generate or validate the API contract.
- Resolve the decisions below before approving this plan; do not infer missing business rules while authoring the contract.

## Scope

- Create the initial OpenAPI 3.x document at `openapi/api.yaml`.
- Specify customer and administrator API operations traceable to the documented functional requirements.
- Define reusable request/response schemas, shared identifiers and date formats, status enums already documented in the domain rules, and RFC 7807 Problem Details.
- Describe bearer-JWT security requirements and the USER/ADMIN access expectations without implementing Keycloak or Spring Security.
- Configure Gradle generation for a Kotlin/Spring backend interface and a TypeScript Angular client, using the generator options documented in the architecture.
- Generate into build directories and validate the specification and backend generated-source compilation without manually editing generated files.
- Document the contract-generation and validation commands.

## Technical Approach

- Keep `openapi/api.yaml` as the only manually maintained API contract. Use the existing `/api/v1` prefix and the API-first architecture.
- Use OpenAPI 3.x and the OpenAPI Generator Gradle plugin. Pin a generator plugin release compatible with Gradle 9.3.0, Kotlin 2.3.21, Spring Boot 3.5.16, JDK 25, and both selected generators.
- Configure the Kotlin backend generator as `kotlin-spring` with `interfaceOnly: true`, `useSpringBoot3: true`, `useTags: true`, `delegatePattern: true`, and `useBeanValidation: true`.
- Configure the client generator as `typescript-angular`. Generate backend and frontend artifacts under `backend/build/generated/openapi/` and `frontend/build/generated/openapi/`; generated output must remain disposable and untracked.
- Set generator options such as `hideGenerationTimestamp: true` where supported so repeated generation does not create needless diffs.
- Add generated Kotlin source directories to the backend Gradle source set so compilation verifies generated interfaces. Add only dependencies required to compile generated output; do not add controller or delegate implementations or endpoint behavior.
- Model customer ownership so booking creation does not accept a client-supplied customer ID; associate the authenticated customer in the later security/application implementation.
- Represent known statuses from `docs/business-rules.md` and fields from `docs/data-model.md`. Do not define cancellation eligibility, invoice-generation timing, rental limits, fees, VAT, payment-provider behavior, or other unresolved rules in the contract.
- Describe operation access using the documented USER/ADMIN roles and bearer authentication, while deferring JWT validation, role mapping, and ownership enforcement to the Security milestone.
- Do not add database migrations, PostgreSQL schema, Keycloak realm/client configuration, Docker Compose services, Angular application scaffolding, or API runtime behavior in this milestone.

## API Contract Coverage

The initial contract should cover the documented capabilities:

- Container-type catalogue and details, and availability queries.
- Customer booking creation, own-booking list/details, cancellation request, and associated invoice retrieval.
- Administrator booking list/filter and status management.
- Administrator management operations for container types, physical containers, customers, and invoices, including manual paid-status updates and physical-container assignment/release.
- Shared Address, Customer, ContainerType, Container, ContainerAssignment, Availability, Booking, Invoice, InvoiceLine, and Problem Detail schemas, limited to documented fields and rules.

Exact paths, HTTP methods, request/response shapes, and catalogue/availability access follow the resolved decisions below.

## Implementation Steps

1. [x] **Resolve API-contract decisions**
   - Decide the endpoint coverage, booking quantity representation, public/authenticated operation boundaries, and contract choices listed under Resolved Decisions.
   - Record only approved behavior in the API specification; preserve unresolved business rules as documented constraints rather than inventing semantics.

2. [x] **Author the initial OpenAPI document**
   - Add `openapi/api.yaml` with API metadata, `/api/v1` paths, tags, stable `operationId` values, reusable schemas, validation constraints supported by requirements, security declarations, and Problem Detail responses.
   - Ensure customer-supplied booking data cannot select or override the owning customer.
   - Add examples only where they do not imply unapproved business rules or production credentials.

3. [x] **Configure Kotlin/Spring generation**
   - Add and pin the OpenAPI Generator Gradle plugin.
   - Configure the Kotlin/Spring generation task with the architecture-required options and generated output directory.
   - Include generated Kotlin sources in the compile source set and add only dependencies required by the generated interfaces.

4. [x] **Configure Angular client generation**
   - Configure a separate `typescript-angular` generation task using the same API document.
   - Write output only beneath `frontend/build/generated/openapi/`; do not create a new Angular application or hand-edit generated files.

5. [x] **Add validation and usage documentation**
   - Provide Gradle tasks/commands to validate the specification, generate both targets, compile backend generated sources, and run the relevant backend tests.
   - Document generated-output locations, regeneration, and the rule that `openapi/api.yaml` must be changed before generated output.
   - Verify generation is repeatable and generated artifacts are not tracked.

## Files and Components Affected

- `openapi/api.yaml` — initial API contract.
- `backend/build.gradle.kts` — generator plugin/tasks, dependencies, and generated Kotlin source integration.
- `backend/build/generated/openapi/**` — generated Kotlin/Spring sources; build output only.
- `frontend/build/generated/openapi/**` — generated TypeScript Angular client; build output only.
- `.gitignore` — exclude generated frontend build output if not already ignored.
- `README.md` — API validation and generation commands.

No manually authored source files under application packages are expected in this milestone.

## Required Database Migrations

None. The OpenAPI contract describes API models only; it must not create or imply persistence schema changes.

## Required API Changes

- Add the initial OpenAPI 3.x specification at `openapi/api.yaml`.
- Generate backend Kotlin/Spring interfaces and an Angular TypeScript client from that specification.
- Do not implement endpoints or change runtime API behavior in this milestone.

## Security Implications

- Use the existing Keycloak/JWT architecture as the documented authentication model.
- Mark protected operations with bearer authentication and identify USER/ADMIN access expectations in operation documentation.
- Do not accept client-supplied customer ownership identifiers for customer booking operations.
- Generated interfaces and security declarations do not enforce authorization; JWT verification, role mapping, and ownership checks remain in the Security and feature milestones.
- Do not include real tokens, secrets, or production issuer configuration.

## Testing Requirements

- Validate `openapi/api.yaml` with the OpenAPI Generator validation task.
- Generate both Kotlin/Spring and TypeScript Angular targets from a clean build.
- Compile the generated Kotlin/Spring interfaces with the existing Gradle/JDK 25 build.
- If an Angular project is not in scope, verify frontend generation and validate the output without claiming an Angular application build.
- Run backend unit tests and confirm generator configuration does not alter application behavior.
- Regenerate twice and verify stable output; confirm generated files stay under ignored build directories and are not manually modified.
- Check that each specified operation maps to a documented functional requirement and that no contract encodes an unresolved business rule.

## Acceptance Criteria

- `openapi/api.yaml` is the documented single source of truth and uses the `/api/v1` prefix.
- All in-scope documented customer and administrator capabilities have specified operations, request/response schemas, and appropriate error responses.
- The contract uses only agreed behavior and documented fields/statuses; unresolved business rules remain open.
- Gradle exposes independent backend and frontend generation tasks using the documented generator options.
- Both generators complete successfully; generated backend sources compile under the pinned JDK 25/Spring Boot setup.
- Generated output is disposable, ignored by version control, and reproducible from the specification.
- No database migrations, Keycloak realm/client setup, Spring Security implementation, API endpoint implementation, or frontend application scaffolding is added.

## Definition of Done

- All implementation steps and acceptance criteria are verified.
- OpenAPI validation, both generation tasks, backend compilation, and relevant tests pass.
- The README instructions match the actual Gradle tasks and output paths.
- `planning/roadmap.md` and this plan are updated only after the approved implementation succeeds.
- Plan status is not changed from DRAFT until explicit approval, and no implementation begins before approval.

## Out of Scope

- Controllers, delegates, use cases, domain or persistence models, repositories, or API runtime behavior.
- PostgreSQL schema, Flyway migrations, database constraints, or data seeding.
- JWT Resource Server implementation, Keycloak realm/client/user provisioning, role mapping, or ownership enforcement.
- Booking, inventory, customer, invoice, cancellation, pricing, VAT, payment, or email business logic.
- Angular application setup, UI components, routing, authentication, or frontend business behavior.
- Dockerfile, Docker Compose, CI/CD, deployment, monitoring, pagination implementation, or Swagger UI hosting.

## Resolved Decisions

- Cover all documented customer and administrator capabilities, including physical-container assignment and release.
- Require a container quantity in booking and availability operations. The later persistence implementation must account for the requested booking quantity; this API-only plan does not change the data model or database.
- Keep catalogue and availability operations public. Protect customer operations with USER bearer-JWT access and administrative operations with ADMIN bearer-JWT access.
- Use OpenAPI 3.0.3 for compatibility with both selected generators.
- Represent monetary amounts as integer cents and include an ISO 4217 alphabetic currency code without restricting the API to one currency.
- Represent container volume in cubic meters (`volumeM3`).
- Use a shared Address with street, house number, postal code, city, and country code.
- Specify cancellation and invoice operations but leave cancellation eligibility, invoice timing, and calculation policy unresolved.
- Generate the Angular client and structurally validate its output; do not scaffold an Angular application in this milestone.
- Include pagination on list operations using zero-based page indices, a default page of 0, a default size of 20, and a maximum size of 100.
