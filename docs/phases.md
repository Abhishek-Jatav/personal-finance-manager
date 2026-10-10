# Implementation Phases
## Personal Finance Manager

Complete each phase’s exit criteria before starting the next. The sequence follows the roadmap and keeps downstream calculations dependent on stable transaction logic.

## Phase 1 — Project setup
**Scope**
- Generate Maven Spring Boot 3 project with Java 17.
- Add Spring Web, Spring Data JPA, Spring Security, H2, Validation, and test dependencies.
- Create the agreed package structure.
- Configure local H2 and application startup.

**Deliverables**
- Application starts locally.
- Database connection works.
- README contains prerequisites and run command.

**Exit criteria**
- `mvn test` passes.
- Application starts without configuration errors.
- H2 console is available only in the local profile.

## Phase 2 — Registration and authentication
**Scope**
- User entity/repository and unique email constraint.
- Registration DTO validation and service.
- Password hashing.
- Session-based login/logout and security configuration.
- Current-user resolver and global error response foundation.

**Exit criteria**
- Registration, login, and logout work.
- Duplicate email returns `409`.
- Password hash is stored, never plaintext.
- Protected route rejects unauthenticated requests.
- Session invalidation and CSRF behavior are tested.

## Phase 3 — Transaction management
**Scope**
- Transaction entity, enum, DTOs, repository, service, controller.
- Create/list/update/delete.
- Date range, category, and type filters.
- Ownership, date immutability, positive amount, and category-type checks.

**Exit criteria**
- CRUD and filters work for an authenticated user.
- Invalid amount/future date/date change are rejected.
- A user cannot access another user’s transaction.
- Deleted transactions no longer appear in lists or derived queries.

## Phase 4 — Category management
**Scope**
- Category entity, default seed strategy, list/create/delete endpoints.
- Type and ownership rules.
- Duplicate detection and referenced-category deletion protection.

**Exit criteria**
- Required defaults are available.
- Defaults cannot be deleted.
- Custom category names are unique per user under the documented normalization policy.
- Categories cannot be used across owners or types.
- Referenced categories cannot be deleted.

## Phase 5 — Savings goals
**Scope**
- Goal entity, DTOs, CRUD endpoints.
- Goal progress query/service calculation.
- Validation of target amount and dates.

**Exit criteria**
- Goal CRUD respects ownership.
- Progress, completion, and remaining amount match hand-calculated fixtures.
- Negative and over-100% completion cases work.
- Editing/deleting transactions changes subsequent goal calculations correctly.

## Phase 6 — Reports and analytics
**Scope**
- Monthly and yearly endpoints.
- Category-level income/expense totals and net savings.
- User scoping and period boundaries.

**Exit criteria**
- Monthly/yearly results match fixtures.
- Empty periods return the documented zero/empty representation.
- Other users’ transactions never affect totals.
- Month/year validation and leap-year boundaries are tested.

## Phase 7 — Validation and error handling
**Scope**
- Global exception advice.
- Stable JSON error schema.
- Mapping of validation, missing-resource, conflict, auth, and unexpected failures.

**Exit criteria**
- Each documented error code is verified.
- Responses contain no stack trace or internal secrets.
- Error payload fields are consistent across controllers.

## Phase 8 — Testing
**Scope**
- JUnit 5 and Mockito service unit tests.
- Repository tests and Spring/MockMvc integration tests.
- Security, validation, isolation, goals, and report coverage.
- JaCoCo report and coverage threshold.

**Exit criteria**
- Required test cases pass.
- Coverage is at least 80% under the chosen JaCoCo metric and scope.
- CI or a reproducible local command runs the full suite.
- Flaky tests and test-order dependencies are removed.

## Phase 9 — Deployment and submission
**Scope**
- Public repository and complete README.
- Externalized configuration and secrets.
- Deployment to Render or another suitable host.
- Run assignment-provided test script against the deployed API.
- Capture test report screenshot and record repository/deployment URLs.

**Exit criteria**
- Deployment health check succeeds.
- Authentication and core endpoints work in the deployed environment.
- Persistence behavior is understood and meets assignment requirements.
- Assignment script passes, and submission artifacts are ready.

## Cross-phase quality gates
- Do not skip tests for ownership rules.
- Do not expose entities directly from controllers.
- Do not add reports before transaction rules are reliable.
- Keep API, database, and test contracts synchronized.
- Record unresolved assumptions rather than silently changing requirements.
