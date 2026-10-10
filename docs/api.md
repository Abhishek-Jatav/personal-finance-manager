# REST API Contract
## Personal Finance Manager

**Base path:** `/api`  
**Authentication:** Spring Security session cookie  
**Content type:** `application/json` for JSON bodies  
**Dates:** ISO-8601 calendar dates (`YYYY-MM-DD`)  
**Types:** `INCOME`, `EXPENSE`

> The roadmap does not specify every request/response field or exact assignment test-script schema. The contracts below are a consistent proposed baseline; reconcile exact field names and response shapes with the supplied assignment PDF/test script before implementation.

## 1. Authentication

### `POST /api/auth/register`
Create an account. Public endpoint.

Example request:
```json
{
  "fullName": "Alex Example",
  "email": "alex@example.com",
  "password": "a-long-password",
  "phoneNumber": "optional"
}
```
Success: `201 Created` with safe user fields (`id`, `fullName`, `email`, `phoneNumber`). Never return password or password hash. Duplicate email: `409 Conflict`. Invalid fields: `400 Bad Request`.

### `POST /api/auth/login`
Authenticate using Spring Security session authentication. Exact request processing may be handled by Spring Security’s configured login filter; if a controller endpoint is used, keep the behavior consistent.

Proposed request:
```json
{"email":"alex@example.com","password":"a-long-password"}
```
Success: `200 OK`, safe user summary, and session cookie set by the server. Failure: `401 Unauthorized`. Session cookie should be HttpOnly and Secure on HTTPS. CSRF token behavior must be documented for clients.

### `POST /api/auth/logout`
Invalidates the current session. Success: `204 No Content` or `200 OK` with a documented message; choose one and keep tests consistent. An invalid/missing session may return `401` or idempotent success according to the selected security configuration.

## 2. Transactions

### `POST /api/transactions`
Create a transaction for the authenticated user.

Proposed request:
```json
{
  "amount": 125.50,
  "date": "2026-10-09",
  "categoryId": 1,
  "description": "Groceries",
  "type": "EXPENSE"
}
```
Success: `201 Created` with transaction response. Reject non-positive amount, future date, category not available to the user, or category/type mismatch.

### `GET /api/transactions`
List transactions owned by the current user. Default sort: date descending, then stable secondary ordering.

Supported query parameters:
- `from=YYYY-MM-DD` — inclusive lower date bound
- `to=YYYY-MM-DD` — inclusive upper date bound
- `categoryId=<id>`
- `type=INCOME|EXPENSE`

Reject invalid dates, unsupported type, or `from > to` with `400`. The initial response may be an array to satisfy assignment expectations. Add pagination only if it does not conflict with the required contract.

### `PUT /api/transactions/{id}`
Update an owned transaction. Baseline assumes full replacement of mutable fields; `date` must match the stored date and cannot be changed. Success: `200 OK`. Missing/not-owned resource: `404`.

### `DELETE /api/transactions/{id}`
Delete an owned transaction. Success: `204 No Content`. Missing/not-owned resource: `404`. Deleted records must not affect goals or reports.

## 3. Categories

### `GET /api/categories`
List default categories plus custom categories owned by the authenticated user. Public defaults and private categories should be distinguishable in the response.

### `POST /api/categories`
Create a custom category.

Proposed request:
```json
{"name":"Pets","type":"EXPENSE"}
```
Success: `201 Created`. Duplicate normalized name: `409 Conflict`. Invalid type/name: `400`.

### `DELETE /api/categories/{name}`
Delete an owned custom category by name, as described in the roadmap. URL-encode the name. Default, other-user, or missing categories must not be deletable. If referenced by a transaction, return `409 Conflict`.

**Implementation note:** A category ID path is generally less ambiguous than a name path, but this baseline preserves `/api/categories/{name}` because that is what the roadmap specifies. Do not silently change the path if an assignment script expects it.

## 4. Savings goals

### `POST /api/goals`
Create a goal.

Proposed request:
```json
{
  "goalName":"Emergency fund",
  "targetAmount":50000.00,
  "targetDate":"2027-06-30",
  "startDate":"2026-10-01"
}
```
Success: `201 Created`. Target must be positive; start date cannot be in the future; target date must be on or after start date.

### `GET /api/goals`
List the authenticated user’s goals, including computed progress fields.

### `GET /api/goals/{id}`
Return one owned goal with computed progress fields. Missing/not-owned: `404`.

### `PUT /api/goals/{id}`
Update permitted goal fields, especially target amount/date as specified in the roadmap. Decide whether the request is full replacement or partial update and document it consistently. Success: `200 OK`.

### `DELETE /api/goals/{id}`
Delete an owned goal. Success: `204 No Content`.

Proposed goal response:
```json
{
  "id": 10,
  "goalName": "Emergency fund",
  "targetAmount": 50000.00,
  "targetDate": "2027-06-30",
  "startDate": "2026-10-01",
  "progress": 2500.00,
  "completionPercentage": 5.00,
  "remaining": 47500.00
}
```
`progress = income - expenses` from the goal start date through the effective current date, inclusive. Do not clamp negative or over-target results.

## 5. Reports

### `GET /api/reports/monthly/{year}/{month}`
Returns one calendar month. Validate `month` is 1–12.

### `GET /api/reports/yearly/{year}`
Returns one calendar year.

Proposed response:
```json
{
  "period": "2026-10",
  "totalIncome": 40000.00,
  "incomeByCategory": [
    {"categoryName":"Salary","total":40000.00}
  ],
  "totalExpenses": 12500.00,
  "expensesByCategory": [
    {"categoryName":"Food","total":5000.00},
    {"categoryName":"Rent","total":7500.00}
  ],
  "netSavings": 27500.00
}
```
`netSavings = totalIncome - totalExpenses`. Empty periods return zero totals and empty category arrays, unless the assignment specifies another shape.

## 6. Error response
Use a consistent response:
```json
{
  "timestamp": "2026-10-10T06:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": [
    {"field":"amount","message":"must be greater than 0"}
  ],
  "path": "/api/transactions"
}
```

| Status | Meaning |
|---|---|
| `400` | Invalid input, malformed JSON, invalid filters |
| `401` | Unauthenticated |
| `403` | Authenticated but forbidden; may include CSRF rejection |
| `404` | Resource missing or not owned |
| `409` | Duplicate/conflicting operation |
| `500` | Unexpected server error; generic client message |

## 7. Security contract
- All routes are protected except explicitly public registration and login; logout requires the configured authentication behavior.
- Session cookies authenticate subsequent requests.
- Cookie-based state-changing requests must satisfy configured CSRF protection.
- Never accept a request’s `userId` as proof of ownership.
- All list and aggregate endpoints are scoped to the authenticated user.

## 8. Status-code consistency
Use `201` for resource creation, `200` for successful reads/updates, and `204` for successful deletion. If assignment tests prescribe another response, follow the assignment and update this contract.
