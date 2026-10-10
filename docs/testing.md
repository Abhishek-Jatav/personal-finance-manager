# Testing Strategy
## Personal Finance Manager

## 1. Objectives
Verify business correctness, authentication, ownership isolation, API contracts, validation, persistence queries, and deployment readiness. Target at least 80% code coverage as required by the roadmap; coverage is a signal, not a substitute for meaningful assertions.

## 2. Test layers
### Unit tests
Use JUnit 5 and Mockito to test services without starting the full application:
- Registration normalization and duplicate-email handling.
- Password encoder usage.
- Transaction amount/date/category/type rules.
- Immutable transaction date.
- Category uniqueness, ownership, and deletion restrictions.
- Goal progress/completion/remaining calculations.
- Monthly/yearly aggregation logic.
- Exception mapping where practical.

### Repository tests
Use Spring Boot test support and an isolated database to verify:
- Owner-scoped lookups.
- Date-range inclusivity.
- Sorting.
- Category reference checks.
- Aggregate query correctness.
- Constraints and seed idempotency.

### API/integration tests
Use MockMvc or an equivalent Spring test client:
- Request validation and JSON serialization.
- Registration/login/logout and session lifecycle.
- CSRF behavior for cookie-authenticated state-changing requests.
- Correct HTTP status and error schema.
- End-to-end ownership isolation across two users.
- Endpoint behavior with empty datasets and boundary dates.

### Deployment smoke tests
Run a small repeatable collection against the deployed base URL. Do not use real personal financial data.

## 3. Required scenarios
| Area | Positive cases | Negative/boundary cases |
|---|---|---|
| Registration | Valid account | Invalid email, blank fields, duplicate normalized email |
| Authentication | Correct login, session retained | Wrong password, unauthenticated protected request, logout invalidation |
| Password | Hash stored and password verifies | Plaintext is not stored or returned |
| Transactions | Create/list/update/delete | Zero/negative amount, future date, changed date, invalid type, category mismatch |
| Filters | Each filter and combinations | `from > to`, invalid date, empty result |
| Ownership | Owner can access own transaction | Other user cannot read/update/delete it |
| Categories | Defaults and custom category | Delete default, duplicate name, cross-user use, delete referenced category |
| Goals | Correct progress calculations | Negative progress, >100% completion, invalid target/date, other-user goal |
| Reports | Known monthly/yearly totals | Empty period, month 0/13, year boundary, cross-user leakage |
| Errors | Stable JSON schema | No stack trace, SQL detail, or secrets |
| Deployment | Auth and core routes work | Session/CSRF/proxy/persistence misconfiguration |

## 4. Calculation fixtures
Use a deterministic date and fixed transactions. Example fixture for one user and period:
- Income: Salary 3,000.00 and other income 500.00.
- Expenses: Food 250.00 and Rent 750.00.
- Expected total income: 3,500.00.
- Expected total expenses: 1,000.00.
- Expected net savings: 2,500.00.

For a goal starting on the fixture date, expected progress is 2,500.00 if all four transactions are in range. Add a second user with large transactions and verify they do not affect the first user’s result.

## 5. Time and money test practices
- Inject a `Clock` rather than relying on wall-clock time.
- Cover today, tomorrow, month-end, year-end, and leap-day behavior.
- Use exact decimal assertions for amounts.
- Assert percentage rounding according to the documented policy.
- Test progress below zero and above target without clamping.
- Test inclusive start/end date behavior.

## 6. Security testing
- Verify every private endpoint rejects unauthenticated requests.
- Verify user IDs in request bodies cannot override the principal.
- Verify foreign resource IDs return the chosen non-disclosing response (`404`).
- Verify session fixation protection and logout invalidation as configured.
- Verify CSRF is required for state-changing requests where configured.
- Verify no response includes password hashes or sensitive security data.

## 7. Coverage
Use JaCoCo Maven plugin. Set the project’s 80% threshold against a clearly defined metric, preferably line coverage for production code, and report the denominator/scope. Exclude generated code only with a documented reason. Do not weaken thresholds simply to make the build pass.

## 8. Test quality gates
- Tests are deterministic and isolated.
- No dependence on test execution order.
- Assertions verify outcomes, not only that no exception occurred.
- Tests cover both valid and invalid paths.
- Repository tests exercise actual queries.
- Integration tests verify security filters and serialization.
- CI/local test command is documented and reproducible.
