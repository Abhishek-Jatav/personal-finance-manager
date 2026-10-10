# Business Rules
## Personal Finance Manager

These rules are authoritative across services, API behavior, tests, and documentation.

## 1. Identity and account rules
1. Email is required, syntactically valid, normalized consistently, and unique.
2. Passwords are never stored in plaintext; use an adaptive password encoder.
3. A client cannot select the owner of a new transaction, category, or goal by submitting a user ID.
4. All user-owned resources are accessible only to their owner.
5. Logging out invalidates the current session.
6. Do not reveal whether another user’s private resource exists.

## 2. Transaction rules
1. Amount must be strictly greater than zero.
2. Amount uses `BigDecimal`; floating-point types are prohibited for money.
3. Date is required and cannot be later than the application’s current business date.
4. The date of an existing transaction is immutable through the update endpoint. If correction of a date is needed, use a separately approved workflow rather than silently changing this rule.
5. Type must be `INCOME` or `EXPENSE`.
6. Category type must equal transaction type.
7. A transaction’s owner is the authenticated user.
8. Category must be a global default category or a custom category owned by the authenticated user.
9. Lists are sorted newest first by transaction date, with a stable secondary sort (for example, ID descending).
10. Date filters are inclusive: `from` and `to` are included. Reject `from > to`.
11. Deleting a transaction removes it from all subsequent reports and goal calculations. This implementation uses physical deletion unless the assignment requires audit retention.
12. Updates must validate the resulting complete transaction, not only the changed fields.

## 3. Category rules
1. Default categories are global and seeded deterministically:
   - Income: `Salary`
   - Expense: `Food`, `Rent`, `Transportation`, `Entertainment`, `Healthcare`, `Utilities`
2. Default categories cannot be renamed or deleted.
3. Custom categories are owned by one user and cannot be used by other users.
4. Custom category names are trimmed and compared case-insensitively for uniqueness within the same user and category type. Preserve a display form after trimming.
5. A custom category cannot duplicate a default category of the same type for that user.
6. A category referenced by one or more transactions cannot be deleted; return `409 Conflict`.
7. Category type is required and immutable after creation in the initial version.
8. Invalid or unknown category references return `404` (or a documented `400` if the API uses validation-style reference errors consistently).

## 4. Savings-goal rules
1. Target amount must be positive.
2. Goal name is required and trimmed.
3. Start date is required and cannot be in the future.
4. Target date is required and must be on or after start date.
5. Goal owner is the authenticated user.
6. Progress includes the owner’s income minus expenses on or after the goal start date and through the effective calculation date, inclusive.
7. Only transactions that satisfy the normal transaction rules count; deleted transactions do not count.
8. Completion percentage = `progress / targetAmount * 100`.
9. Remaining = `targetAmount - progress`.
10. Do not clamp progress, completion percentage, or remaining amount: negative progress and completion above 100% are valid computed outcomes. A UI may display them thoughtfully, but the API returns the actual calculation.
11. Updating a goal changes only permitted goal fields (target amount and/or target date, plus name only if explicitly included in the implemented contract); it does not rewrite transaction history.
12. Goal calculations never include another user’s transactions.

## 5. Reporting rules
1. Monthly period begins on the first day of the requested month and ends on its final day, inclusive.
2. Yearly period begins January 1 and ends December 31, inclusive.
3. Totals are scoped to the authenticated user.
4. Income and expense totals are grouped by category and transaction type.
5. Net savings = total income − total expenses.
6. A report with no transactions returns zero totals and empty or zero-valued category groups according to one consistent documented response contract.
7. Validate month in the range 1–12 and reject invalid years according to the application’s supported date range.
8. Report aggregation must be based on the same transaction source of truth used by savings goals.

## 6. Validation and error rules
- `400 Bad Request`: malformed JSON, invalid fields, invalid date range, invalid month, unsupported enum value.
- `401 Unauthorized`: missing or invalid authentication.
- `403 Forbidden`: authenticated but explicitly forbidden operation, including CSRF rejection where applicable according to security configuration.
- `404 Not Found`: absent or not-owned private resource.
- `409 Conflict`: duplicate email/category, deleting a referenced category, or other state conflict.
- `500 Internal Server Error`: unexpected server error; return a generic message and log details securely.
- Error response shape is defined in `api.md`; do not return stack traces to clients.

## 7. Security and privacy rules
- Never log passwords, session IDs, authentication cookies, CSRF tokens, or full sensitive request bodies.
- Do not return password hashes or internal security fields.
- Use HTTPS in production.
- Disable H2 console outside local development.
- Configure session cookies with `HttpOnly`, `Secure` in HTTPS environments, and an appropriate `SameSite` policy.
- Keep CSRF protection enabled for cookie-based state-changing requests and document client behavior.
- Avoid using user-provided IDs as authorization evidence.

## 8. Determinism
All monetary aggregation must use decimal arithmetic. Use a single documented rounding rule for derived percentages (recommended: round to 2 decimal places, `HALF_UP`, for display/response only; retain full precision for internal calculations). All time-based calculations use the configured business timezone and an injectable clock in tests.
