# Architecture
## Personal Finance Manager

## 1. Architectural style
Use a pragmatic layered architecture with dependency direction:

`Controller → Service → Repository → Database`

Controllers handle HTTP concerns; services own business rules and transaction boundaries; repositories encapsulate persistence. Entities represent persisted state, DTOs define API contracts, and security configuration defines authentication and authorization. This is compatible with Clean Architecture and SOLID without introducing unnecessary ports/adapters for a small assignment.

## 2. Runtime components
- **Spring Web:** REST routing, request binding, JSON serialization.
- **Spring Security:** authentication, session lifecycle, authorization, CSRF/session controls.
- **Spring Data JPA / Hibernate:** ORM and repository abstraction.
- **H2:** local development and automated-test database.
- **Jakarta Bean Validation:** request validation.
- **Maven:** dependency and build management.
- **JUnit 5, Mockito, Spring Boot Test, MockMvc:** unit and API/integration tests.
- **JaCoCo:** coverage measurement.

## 3. Package structure
```text
com.example.finance
├── FinanceApplication.java
├── config/       # application and security configuration
├── controller/   # REST endpoints and HTTP mapping
├── service/      # business rules and transaction boundaries
├── repository/   # Spring Data repository interfaces
├── entity/       # JPA entities and enums
├── dto/          # request/response DTOs
├── security/     # principal/current-user access helpers
└── exception/    # domain exceptions and global error handling
```
Keep classes cohesive. Do not place business calculations in controllers or expose JPA entities directly as API responses.

## 4. Main responsibilities
### Controllers
- Define route, method, status code, and request/response DTO.
- Apply request validation.
- Delegate to services.
- Avoid database access and business calculations.

### Services
- Enforce ownership and business invariants.
- Coordinate repository calls.
- Define read/write transaction boundaries.
- Calculate goal progress and report aggregates.
- Throw meaningful domain exceptions.

### Repositories
- Provide persistence operations and explicit user-scoped queries.
- Prefer methods that include owner identity in their query predicates.
- Avoid fetching all users’ records and filtering them in application memory.

### Security
- Configure session-based authentication.
- Hash passwords using a supported adaptive encoder such as BCrypt.
- Resolve the authenticated user from the security context.
- Protect state-changing cookie-authenticated requests with CSRF protection; document how clients obtain/send CSRF tokens.
- Configure production cookie flags and session fixation protection.

### Exception layer
- Use `@RestControllerAdvice`.
- Map validation, missing-resource, authentication/authorization, duplicate/conflict, and unexpected errors to a consistent JSON shape.
- Log unexpected failures server-side with a correlation/request ID, without leaking secrets to clients.

## 5. Domain model
- `User` owns transactions, custom categories, and savings goals.
- `Category` is either a global default or a user-owned custom category. It has a type (`INCOME` or `EXPENSE`).
- `Transaction` belongs to exactly one user and references a category whose type matches its transaction type.
- `SavingsGoal` belongs to exactly one user.
- Reports are derived from transactions; totals are not separately persisted in this version.

Use explicit enums for transaction/category type. Use `BigDecimal` for amounts and a consistent scale/rounding policy. IDs should be opaque to clients; database-generated numeric IDs are acceptable for this assignment.

## 6. Request lifecycle
1. Spring Security authenticates the session and establishes the principal.
2. Controller validates and binds the request DTO.
3. Service resolves the current user and applies ownership/business rules.
4. Repository executes owner-scoped query or mutation.
5. Service maps entities to response DTOs or computes derived values.
6. Controller returns the documented HTTP status and JSON response.
7. Global exception handling translates expected failures into stable error responses.

## 7. Ownership pattern
For any resource ID, load the resource using both its identifier and the authenticated user where possible, e.g. a conceptual `findByIdAndUserId`. If no owned resource is found, return `404` to avoid confirming that another user’s resource exists. Reserve `403` for cases where a known authenticated user is explicitly prohibited by policy and resource existence is not sensitive. Never trust a user ID supplied by the client as the authority for ownership.

## 8. Data and transaction boundaries
- Wrap multi-step writes in service-level `@Transactional` methods.
- Use read-only transactions for read operations where beneficial.
- Deleting a transaction must immediately remove it from derived calculations because reports and goals query current transaction data.
- Prevent category deletion if any transaction references it.
- Consider database constraints for uniqueness and required relationships in addition to service validation.

## 9. Environments
- **Local:** H2 in-memory database, development console enabled only locally, verbose diagnostics allowed without sensitive data.
- **Test:** isolated H2/test configuration and deterministic fixtures.
- **Production/demo:** configure secrets and database via environment variables; disable H2 console; use durable storage if persistence across restarts is required. Confirm assignment host support before selecting a production database.

## 10. Architecture decisions
| Decision | Rationale |
|---|---|
| Session authentication | Matches the roadmap and supports standard Spring Security controls |
| Layered architecture | Simple, testable, assignment-aligned |
| DTOs separate from entities | Prevents accidental data exposure and stabilizes API contracts |
| `BigDecimal` for money | Avoids binary floating-point rounding errors |
| Derived reports/goals | Avoids stale aggregate values after edits/deletions |
| H2 for local/test | Low-friction setup; not a durability strategy |
| Central exception handler | Consistent, testable API errors |

## 11. Scalability path (only when needed)
Add pagination, database indexes, schema migrations (Flyway or Liquibase), durable production database, rate limiting, audit events, metrics, and API versioning when usage or operational requirements justify them. Do not add these before core correctness is complete unless the deployment environment requires them.
