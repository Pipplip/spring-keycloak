
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
- [ ] Email notifications (deferred to FEAT-001 in the backlog)

## Phase 8: Hardening
- [ ] Validation
- [ ] Error handling
- [ ] Logging
- [ ] Pagination
- [ ] Extended tests

## Phase 9: Frontend
- [ ] Angular project
- [ ] Responsive shared layout and accessible component theme
- [ ] Keycloak login and role-aware navigation
- [ ] Integrate generated OpenAPI client
- [ ] Customer UI
- [ ] Admin UI
- [ ] API error/loading states and frontend tests
- [ ] Integration with backend

## Phase 10: Deployment
- [ ] Dockerfile
- [ ] Complete Docker Compose
- [ ] Health checks
- [ ] CI pipeline
- [ ] README
- [ ] Swagger UI
