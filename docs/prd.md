# Product Requirements Document (PRD)
## Personal Finance Manager

**Status:** Initial implementation baseline  
**Version:** 1.0  
**Product type:** Authenticated REST API  
**Technology baseline:** Java 17, Spring Boot 3, Maven, Spring Data JPA, Spring Security, H2 for local development and tests

## 1. Product summary
Personal Finance Manager enables each registered user to record income and expenses, organize transactions by category, track savings goals, and review monthly and yearly financial summaries. The system exposes a REST API and uses session-based authentication. User-owned data must remain isolated.

## 2. Goals
- Provide reliable registration, login, and logout.
- Support transaction CRUD, filtering, and validation.
- Support built-in and user-created categories with ownership rules.
- Calculate savings-goal progress from current transactions.
- Produce monthly and yearly income, expense, and net-savings reports.
- Deliver consistent error responses and automated tests.
- Provide a documented deployment and submission process.

## 3. Non-goals
- Bank account aggregation, payment processing, or automatic transaction imports.
- Investment advice, credit scoring, tax filing, or financial recommendations.
- Multi-currency conversion; the initial release uses one configured currency without conversion.
- A web or mobile UI; this scope is a REST API.
- Shared household accounts, roles beyond the user account, or administrator dashboards.

## 4. Users and primary journeys
### Personal user
1. Registers with valid details.
2. Logs in and receives an authenticated session cookie.
3. Creates categories if needed.
4. Records and edits income or expense transactions.
5. Reviews transaction history using filters.
6. Creates a savings goal and checks calculated progress.
7. Reviews monthly and yearly reports.
8. Logs out, invalidating the session.

## 5. Functional requirements
### FR-1 Identity and sessions
- Register a user with full name, email, password, and optional phone number.
- Email must be unique, normalized consistently, and validated.
- Store only a strong password hash; never store or log plaintext passwords.
- Authenticate using Spring Security sessions.
- Logout invalidates the session.
- Unauthenticated requests to protected resources return `401`.
- Every user-scoped query and mutation must enforce ownership.

### FR-2 Transactions
- Create, list, update, and delete transactions.
- Each transaction has an ID, positive amount, date, category, description, type, and owner.
- Use `BigDecimal` for money.
- Date must not be in the future according to the application’s configured business timezone.
- Updating a transaction must not change its date.
- Filter by date range, category, and transaction type; return newest first by default.
- Deleted transactions must not contribute to reports or savings-goal calculations.

### FR-3 Categories
- Provide default categories: Income `Salary`; expenses `Food`, `Rent`, `Transportation`, `Entertainment`, `Healthcare`, `Utilities`.
- Users can list available categories and create/delete their own custom categories.
- Default categories cannot be modified or deleted.
- Custom names must be unique per user, using a documented case-normalization policy.
- A category referenced by a transaction cannot be deleted.
- Category type must match the transaction type.
- A user cannot use another user’s private category.

### FR-4 Savings goals
- Create, list, retrieve, update, and delete goals.
- Goal fields: ID, name, target amount, target date, start date, owner.
- Progress is income minus expenses belonging to the goal owner from the goal start date through the report’s effective date.
- Completion percentage is progress divided by target amount times 100.
- Remaining amount is target amount minus progress.
- Progress is computed from transactions at read time, not stored as a stale aggregate.
- Negative progress and progress above target must be represented without arithmetic errors.

### FR-5 Reports
- Monthly report endpoint: `GET /api/reports/monthly/{year}/{month}`.
- Yearly report endpoint: `GET /api/reports/yearly/{year}`.
- Include income totals by category, expense totals by category, and net savings.
- Reports must use only the authenticated user’s transactions and exclude deleted transactions.
- Report period boundaries must be explicit and timezone-safe.

### FR-6 Validation and errors
- Validate request DTOs with Jakarta Bean Validation.
- Return consistent JSON error payloads.
- Expected status codes: `400` invalid input, `401` unauthenticated, `403` forbidden, `404` missing resource, `409` duplicate email/category or conflicting operation.
- Do not expose stack traces, SQL, secrets, or internal implementation details.

## 6. Non-functional requirements
- **Security:** password hashing, CSRF protection appropriate to cookie-based sessions, secure session settings in production, least privilege, and strict ownership checks.
- **Maintainability:** layered Controller → Service → Repository architecture, clear DTO/entity separation, small cohesive services.
- **Correctness:** decimal money arithmetic, deterministic report boundaries, validation at API and service boundaries where needed.
- **Testability:** JUnit 5 and Mockito unit tests plus Spring integration/API tests; target at least 80% code coverage as required by the assignment.
- **Observability:** structured logs without sensitive values; useful health/readiness checks where hosting supports them.
- **Documentation:** endpoint contracts, configuration, setup, tests, and deployment steps documented.
- **Performance:** indexes for frequent owner/date/category lookups where supported; paginate transaction lists before production-scale use.

## 7. Assumptions and decisions
- One account represents one individual user.
- Initial money values use a single configured currency; the currency code must be documented in API and README. No currency conversion is performed.
- `INCOME` and `EXPENSE` are explicit transaction types and must agree with the selected category type.
- The supplied roadmap uses in-memory H2 (`jdbc:h2:mem:financedb`) for development. Production persistence must be confirmed against hosting and assignment requirements; in-memory H2 is not durable across restarts.
- Session authentication is retained rather than introducing JWT.
- The goal’s progress period starts at `startDate` and ends at the current date or the requested reporting date, inclusive.
- Goal target dates are planning metadata; this baseline does not require transactions to stop counting after the target date.

## 8. Acceptance criteria
- A user can register, log in, use protected endpoints, and log out.
- Duplicate emails are rejected consistently.
- Users cannot read, modify, or delete another user’s transactions, categories, or goals.
- Invalid amounts, future transaction dates, invalid types, and malformed dates are rejected.
- Transaction filters return correct, newest-first results.
- Default categories are present and protected from deletion.
- Referenced categories cannot be deleted.
- Goal progress and completion values match independently calculated fixtures, including negative and over-100% progress.
- Monthly/yearly totals match known transaction fixtures and exclude other users’ data.
- API errors follow the documented JSON schema and status mapping.
- Automated tests run in CI or the documented local workflow, with at least 80% coverage measured using JaCoCo.
- README and deployment instructions allow another developer to run and verify the service.

## 9. Release criteria
Release only when all required endpoints, ownership rules, validation, test coverage, documentation, and assignment verification steps pass. Do not treat a successful application startup as completion.
