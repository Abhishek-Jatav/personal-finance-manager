# Project Roadmap
## Personal Finance Manager

## 1. Product objective
Build a tested, session-authenticated REST API using Java 17, Spring Boot 3, Maven, Spring Data JPA, Spring Security, and H2 for local development. Follow the required Controller → Service → Repository layering.

## 2. Milestones
- [ ] **1. Project setup and H2** — generate project, dependencies, packages, local configuration.
- [ ] **2. Authentication** — registration, login, logout, sessions, password hashing.
- [ ] **3. Transactions** — CRUD, filters, validation, ownership.
- [ ] **4. Categories** — seeded defaults, custom categories, ownership and deletion rules.
- [ ] **5. Savings goals** — goal CRUD and derived progress calculations.
- [ ] **6. Reports** — monthly/yearly category totals and net savings.
- [ ] **7. Error handling** — consistent JSON errors and status codes.
- [ ] **8. Testing** — JUnit 5, Mockito, integration tests, at least 80% coverage.
- [ ] **9. Deployment** — public GitHub repository, hosted API, assignment script, README and screenshot.

## 3. Recommended execution order
### Milestone 1 — Setup
1. Create the project through Spring Initializr.
2. Select Maven, Java 17, Spring Boot 3.x, Jar packaging.
3. Add Spring Web, Spring Data JPA, Spring Security, H2, and Validation.
4. Create the package structure from `architecture.md`.
5. Configure local H2 and confirm startup.

**Definition of done:** application starts and tests execute.

### Milestone 2 — Authentication
1. Implement user persistence and unique normalized email.
2. Implement validated registration.
3. Configure password hashing.
4. Configure session-based authentication and logout.
5. Add current-user resolution and protect private routes.

**Definition of done:** registration/login/logout and session access tests pass.

### Milestone 3 — Transactions
1. Define transaction entity, DTOs, enum, repository.
2. Implement create/list/update/delete.
3. Add date/category/type filters and deterministic sorting.
4. Add validation and ownership checks.

**Definition of done:** CRUD, filter, date immutability, and cross-user tests pass.

### Milestone 4 — Categories
1. Define category model and default seed data.
2. Implement list and custom category creation.
3. Implement custom category deletion with reference checks.
4. Enforce type compatibility and ownership.

**Definition of done:** defaults, uniqueness, type, and deletion restrictions pass.

### Milestone 5 — Savings goals
1. Define goal model and DTOs.
2. Implement goal CRUD.
3. Calculate progress from current transactions.
4. Return completion percentage and remaining amount.

**Definition of done:** positive, negative, and over-target calculations pass.

### Milestone 6 — Reports
1. Implement monthly period boundaries.
2. Implement yearly period boundaries.
3. Aggregate income and expenses by category.
4. Calculate net savings.

**Definition of done:** fixture totals match and user isolation holds.

### Milestone 7 — Error handling
1. Add global exception advice.
2. Standardize error response fields and codes.
3. Map validation and domain failures to status codes.
4. Hide internal details.

**Definition of done:** status/schema tests pass.

### Milestone 8 — Quality and coverage
1. Add service unit tests.
2. Add repository and API integration tests.
3. Add security and isolation tests.
4. Configure JaCoCo and reach at least 80% coverage.
5. Fix flaky tests and document the verification command.

**Definition of done:** complete suite passes and coverage report is reproducible.

### Milestone 9 — Deployment and submission
1. Create a public repository.
2. Write setup and API documentation.
3. Confirm whether deployed data must persist.
4. Configure production environment and secrets.
5. Deploy to Render or another suitable host.
6. Run the provided assignment script.
7. Capture the test report and record URLs.

**Definition of done:** deployment is reachable, required checks pass, and submission artifacts are ready.

## 4. Cross-cutting rules
- Keep API contracts synchronized with `api.md`.
- Keep business rules synchronized with `rules.md`.
- Do not expose entities directly.
- Do not trust client-provided user IDs.
- Use `BigDecimal` for all money.
- Test every ownership boundary.
- Calculate reports and goals from current transactions.
- Keep H2 console local-only.
- Treat in-memory H2 as non-persistent.
- Resolve assignment PDF/test-script ambiguities before locking exact request/response shapes.

## 5. Final completion checklist
- [ ] All endpoints implemented and documented.
- [ ] Authentication and session lifecycle verified.
- [ ] Transaction, category, goal, and report rules tested.
- [ ] Consistent error responses implemented.
- [ ] Cross-user isolation verified.
- [ ] At least 80% coverage achieved and measured.
- [ ] README and environment instructions complete.
- [ ] Deployment verified with the assignment script.
- [ ] Repository URL, API URL, and test report screenshot ready.
