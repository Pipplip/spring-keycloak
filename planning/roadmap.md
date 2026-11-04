
# Development Roadmap

## Phase 1: Project Setup
- [x] Gradle project
- [x] Spring Boot configuration
- [x] PostgreSQL
- [x] Keycloak
- [x] Docker Compose

## Phase 2: OpenAPI
- [x] Generate initial API specification
- [x] Define endpoints and schemas
- [x] Configure OpenAPI Generator
- [x] Validate generated code

## Phase 3: Persistence
- [x] Domain entities
- [x] JPA repositories
- [x] Flyway migrations
- [x] Database constraints

## Phase 4: Security
- [x] Keycloak integration
- [x] JWT Resource Server
- [x] Role mapping
- [x] Customer ownership
- [x] Security tests

## Phase 5: Customer Features
- [x] Container types
- [x] Availability
- [x] Create bookings
- [x] List bookings
- [x] Cancel bookings

## Phase 6: Admin Features
- [x] Booking management
- [x] Container inventory
- [x] Container type management
- [x] Status management
- [x] Customer administration (approved addition)

## Phase 7: Invoicing
- [x] Invoice creation
- [x] Invoice calculation
- [x] Invoice status
- [x] Invoice retrieval and administration
- [ ] Email notifications (deferred to FEAT-002 in the backlog)

## Phase 8: Hardening
- [x] Validation
- [x] Error handling
- [x] Logging
- [x] Pagination
- [x] Extended tests

## Phase 9: Frontend
- [x] Angular project
- [x] Responsive shared layout and accessible component theme
- [x] Keycloak login and role-aware navigation
- [x] Integrate generated OpenAPI client
- [x] Customer UI
- [x] Admin UI
- [x] API error/loading states and frontend tests
- [x] Integration with backend

## Phase 10: Deployment
- [x] Dockerfile
- [x] Complete Docker Compose
- [x] Health checks
- [x] CI pipeline configuration
- [x] README
- [x] Swagger UI

PLAN-010 remains IN PROGRESS: interactive Keycloak browser login and
authenticated USER/ADMIN workflows, and execution of the CI workflow on GitHub,
still require verification.

PLAN-009 remains IN PROGRESS pending authenticated customer/administrator
browser verification and execution of the CI workflow on GitHub.

PLAN-011 is COMPLETED after backend/frontend checks and the isolated local
Keycloak registration/profile browser workflow passed. PLAN-009 and PLAN-010
remain in progress for their separately tracked verification.

## Phase 11: Customer Registration and Profiles
- [x] Keycloak-hosted self-registration with automatic USER assignment
- [x] Local customer profile onboarding and current-profile API
- [x] Customer profile completion and editing UI
- [x] Profile ownership and authorization tests
- [x] Documentation and local setup
