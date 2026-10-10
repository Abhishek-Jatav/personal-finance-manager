# Coding Standard
## Personal Finance Manager

## 1. General
- Target Java 17 and Spring Boot 3.
- Use Maven and keep dependencies version-managed by the Spring Boot parent/BOM.
- Follow standard Java naming: `UpperCamelCase` types, `lowerCamelCase` members, uppercase enum constants.
- Keep classes small, focused, and testable.
- Prefer constructor injection; avoid field injection.
- Avoid static mutable state and unnecessary utility classes.
- Do not add application code to controllers beyond HTTP mapping and delegation.

## 2. Layering
- Controllers depend on services, not repositories.
- Services own business rules and transaction boundaries.
- Repositories perform persistence and user-scoped querying.
- DTOs define external contracts; entities define persistence.
- Do not return entities directly from controllers.
- Use explicit mapper methods or a mapping library only if mapping volume justifies it.

## 3. Naming
- Controllers: `TransactionController`
- Services: `TransactionService`
- Repositories: `TransactionRepository`
- Request DTOs: `CreateTransactionRequest`, `UpdateTransactionRequest`
- Response DTOs: `TransactionResponse`
- Domain exceptions: `ResourceNotFoundException`, `ConflictException`
- Enums: `TransactionType.INCOME`, `TransactionType.EXPENSE`

## 4. Validation
- Apply Jakarta Bean Validation to request DTOs (`@NotBlank`, `@Email`, `@Positive`, `@NotNull`, size constraints).
- Use service-level checks for rules requiring database state, such as duplicate category names, category ownership, and referenced-category deletion.
- Validate date ranges and enum/category compatibility.
- Keep validation messages safe and actionable.
- Do not rely exclusively on controller validation for invariants that can be bypassed by non-HTTP service calls.

## 5. Money, dates, and types
- Use `BigDecimal`, never `float` or `double`, for money.
- Avoid constructing `BigDecimal` from a binary floating-point value.
- Define scale and rounding explicitly.
- Use `LocalDate` for calendar dates.
- Inject `Clock` for current-date logic so tests can be deterministic.
- Use enums rather than magic strings for transaction/category types.
- Avoid nullable values where a required domain value exists.

## 6. Persistence
- Put database access behind Spring Data repositories.
- Use explicit owner-scoped query methods for private resources.
- Add unique constraints for normalized email and appropriate category uniqueness.
- Avoid N+1 query patterns in reports and goal calculations; use aggregate queries when they remain clear and correct.
- Keep relationship fetch strategies intentional; avoid indiscriminate eager loading.
- Use `@Transactional` at service methods for multi-step writes.
- Do not expose database exceptions directly to clients.

## 7. Security
- Use an adaptive password encoder.
- Never log credentials, cookies, CSRF tokens, password hashes, or sensitive request bodies.
- Do not trust client-supplied owner IDs.
- Keep CSRF protections appropriate to session-based authentication.
- Use secure cookie settings in deployed HTTPS environments.
- Disable H2 console in non-local environments.
- Use environment variables or the host’s secret manager for deployment secrets.

## 8. Error handling and logging
- Centralize HTTP error mapping with `@RestControllerAdvice`.
- Use domain-specific exceptions instead of returning null or arbitrary strings.
- Use stable error codes and appropriate HTTP statuses.
- Log unexpected failures with context and correlation identifiers, without sensitive data.
- Do not swallow exceptions silently.
- Never send stack traces to clients.

## 9. Testing
- Name tests by behavior, e.g. `createTransaction_rejectsFutureDate`.
- Follow Arrange–Act–Assert.
- Unit-test service rules with JUnit 5 and Mockito.
- Use integration tests for JPA queries, validation, serialization, and security behavior.
- Use isolated fixtures and avoid order-dependent tests.
- Include positive, negative, boundary, and cross-user cases.
- Enforce coverage with JaCoCo; document the exact metric and packages included.

## 10. Code quality gates
Before merging:
- `mvn test` passes.
- Formatting and static checks, if configured, pass.
- No secrets are committed.
- API and docs match implementation.
- Ownership checks are present for every private-resource operation.
- Test coverage target is met.
- New dependencies have a clear purpose and compatible licenses.

## 11. Dependency discipline
Prefer mature Spring ecosystem libraries. Do not introduce Lombok, MapStruct, migrations, container orchestration, or extra abstractions by default; add them only when they materially improve maintainability or are required by the deployment environment.
