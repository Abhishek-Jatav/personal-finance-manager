# Design Specification
## Personal Finance Manager

## 1. Design principles
- **Clarity over novelty:** predictable REST semantics and simple domain models.
- **Consistency:** shared validation, error payloads, naming, date rules, and money precision.
- **Secure by default:** authenticate protected endpoints and scope every resource to its owner.
- **Derived truth:** calculate reports and goal progress from current transactions.
- **Low coupling:** controllers depend on services; services depend on repositories and domain rules.

## 2. Domain concepts
### User
Represents an individual account. Fields: ID, full name, normalized email, password hash, optional phone number, created timestamp if useful. The API never exposes the password hash.

### Category
Fields: ID, name, type, default flag, owner for custom categories. A default category has no user owner and is globally available; a custom category belongs to exactly one user. The implementation must avoid confusing a null owner with a missing/invalid category.

### Transaction
Fields: ID, amount (`BigDecimal`), date (`LocalDate`), category reference, description, type (`INCOME`/`EXPENSE`), owner. Prefer `LocalDate` for day-based transaction dates to avoid accidental UTC date shifts.

### SavingsGoal
Fields: ID, goal name, target amount (`BigDecimal`), target date (`LocalDate`), start date (`LocalDate`), owner. Progress fields are calculated in responses and are not stored as authoritative state.

## 3. Money and date policy
- Use `BigDecimal` for every amount and aggregate.
- Reject zero and negative transaction amounts and non-positive target amounts.
- Define a single currency for the initial release and document it; no conversion is performed.
- Use `LocalDate` for transaction/start/target dates.
- Use an injected `Clock` and configured business timezone for “today” validation.
- Date ranges are inclusive.
- For report queries, use start-inclusive and end-exclusive boundaries internally where that avoids database timestamp ambiguity; if dates are stored as `LocalDate`, compare inclusive date bounds directly.
- Round calculated completion percentage to two decimal places using `HALF_UP` for API display; document this choice.

## 4. Category seed and normalization
Seed required defaults idempotently on startup or via a deterministic data initializer:
- `Salary` — `INCOME`
- `Food`, `Rent`, `Transportation`, `Entertainment`, `Healthcare`, `Utilities` — `EXPENSE`

Do not recreate duplicate defaults on every restart. For custom names, trim leading/trailing whitespace and use case-insensitive uniqueness within the same user and type. Keep the canonical display spelling as entered after trimming.

## 5. API response design
- Use request DTOs and response DTOs.
- Return resource IDs and stable enum values.
- Use ISO-8601 dates (`YYYY-MM-DD`).
- Represent money as JSON numbers serialized from `BigDecimal`; clients should treat them as decimal values and avoid binary floating-point arithmetic for financial calculations.
- Return clear status codes and a consistent error schema.
- List endpoints should be prepared for pagination; initial contract may return a simple array if assignment tests require it. Do not change response shape without updating `api.md` and tests.

## 6. Suggested response fields
### Transaction
`id`, `amount`, `date`, `category` (object or stable category ID/name according to the chosen API contract), `description`, `type`.

### Goal
`id`, `goalName`, `targetAmount`, `targetDate`, `startDate`, `progress`, `completionPercentage`, `remaining`.

### Report
`period`, `totalIncome`, `incomeByCategory`, `totalExpenses`, `expensesByCategory`, `netSavings`.

## 7. Error payload design
Use a stable structure:
```json
{
  "timestamp": "2026-10-10T06:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": [
    {"field": "amount", "message": "must be greater than 0"}
  ],
  "path": "/api/transactions"
}
```
`fieldErrors` may be empty or omitted for non-field errors, but the chosen contract must be consistent. Do not return stack traces.

## 8. Security design
- Session cookie is HttpOnly; Secure on HTTPS; use an appropriate SameSite value.
- Keep CSRF enabled for session-authenticated state-changing requests. API clients must fetch and send a CSRF token according to Spring Security configuration.
- Password encoder is adaptive (BCrypt is acceptable).
- H2 console is local-profile only and should be excluded from production.
- Never serialize JPA entities directly; explicitly map response fields.
- Do not include password hash, session data, or internal ownership metadata unless intentionally part of a safe response.

## 9. UX for API consumers
Although no UI is in scope, API messages should make correction straightforward:
- Identify invalid field names.
- Explain date-range and category-type errors.
- Return stable error codes for client handling.
- Avoid overly detailed responses that reveal other users’ data.

## 10. Open decisions to confirm
- Assignment’s exact JSON shapes and test-script expectations.
- Required currency and scale.
- Whether list endpoints require pagination.
- Whether deployment must preserve data across restarts.
- Whether transaction deletion is physical or soft deletion.
- Whether update requests are full replacement (`PUT`) or partial updates; this baseline treats `PUT` as full replacement except immutable date.
