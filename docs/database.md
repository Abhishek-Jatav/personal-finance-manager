# Database Design
## Personal Finance Manager

## 1. Database strategy
Use H2 in-memory for local development and automated tests, as specified in the roadmap. The URL `jdbc:h2:mem:financedb` loses data when the application stops. Do not rely on this configuration for durable live deployment. Confirm assignment persistence expectations and host support before choosing a production database.

## 2. Entities and fields
Types below are logical recommendations; align exact column names and constraints with implementation.

### `users`
| Column | Type | Rules |
|---|---|---|
| `id` | generated numeric ID | Primary key |
| `full_name` | varchar | Required |
| `email` | varchar | Required, normalized, unique |
| `password_hash` | varchar | Required; never plaintext |
| `phone_number` | varchar | Optional |
| `created_at` | timestamp | Optional audit field |

### `categories`
| Column | Type | Rules |
|---|---|---|
| `id` | generated numeric ID | Primary key |
| `name` | varchar | Required, trimmed |
| `type` | enum/string | `INCOME` or `EXPENSE` |
| `is_default` | boolean | Required |
| `user_id` | FK to users, nullable for defaults | Null only for global defaults |

Recommended uniqueness policy: enforce normalized category name uniqueness per owner and type. Because nullable unique constraints differ by database, implement service checks and suitable indexes/constraints for the selected database. Seed default categories idempotently.

### `transactions`
| Column | Type | Rules |
|---|---|---|
| `id` | generated numeric ID | Primary key |
| `amount` | decimal | Required, positive |
| `transaction_date` | date | Required, not future by business rule |
| `category_id` | FK to categories | Required |
| `description` | varchar/text | Optional or required per final API contract |
| `type` | enum/string | `INCOME` or `EXPENSE` |
| `user_id` | FK to users | Required |

The service must verify that the category is a default or belongs to the same user and that category type matches transaction type.

### `savings_goals`
| Column | Type | Rules |
|---|---|---|
| `id` | generated numeric ID | Primary key |
| `goal_name` | varchar | Required |
| `target_amount` | decimal | Required, positive |
| `target_date` | date | Required, on/after start date |
| `start_date` | date | Required, not future |
| `user_id` | FK to users | Required |

Do not store progress, completion percentage, or remaining amount as authoritative columns. Calculate them from transactions to prevent stale totals.

## 3. Relationships
- User 1 → many Transactions.
- User 1 → many custom Categories.
- User 1 → many SavingsGoals.
- Category 1 → many Transactions, but category deletion is blocked while referenced.
- Default categories have no user owner; custom categories have one owner.

## 4. Indexes and constraints
Recommended:
- Unique index on normalized user email.
- Index on `(user_id, transaction_date)`.
- Index on `(user_id, type, transaction_date)`.
- Index on `(category_id)` for reference checks.
- Index on `(user_id, start_date)` for goals if needed.
- Foreign keys for ownership and category references.
- `NOT NULL` for required fields.
- Positive amount constraints where supported, in addition to application validation.

The exact expression-index syntax for case-insensitive category uniqueness varies by database. Do not assume an H2-specific constraint will transfer unchanged to a production database.

## 5. Deletion behavior
- User deletion is out of scope; do not cascade-delete financial data unless a future requirement explicitly defines account deletion behavior.
- Transaction deletion removes the transaction from future goal/report calculations.
- Category deletion is rejected while referenced; never cascade-delete transactions as a side effect.
- Goal deletion removes the goal but leaves transaction history unchanged.
- Default categories cannot be deleted.

## 6. Query requirements
All private queries must include authenticated user scope. Typical conceptual operations:
- Find transaction by ID and owner.
- Find goals by owner.
- Find custom category by normalized name, owner, and type.
- Sum income/expenses by owner and date period.
- Sum goal-period income/expenses by owner and date from goal start date.

Reports may use repository aggregate queries or service aggregation. Prefer aggregate queries for larger datasets while preserving clarity and testability.

## 7. Seed data
Seed defaults idempotently:
- `Salary` — `INCOME`
- `Food` — `EXPENSE`
- `Rent` — `EXPENSE`
- `Transportation` — `EXPENSE`
- `Entertainment` — `EXPENSE`
- `Healthcare` — `EXPENSE`
- `Utilities` — `EXPENSE`

Seeding must not create duplicates on application restart.

## 8. Schema evolution
For the initial assignment, Hibernate schema generation may use `ddl-auto=update` locally as in the roadmap. Treat that as a development convenience, not a production migration strategy. If a durable production database is introduced, prefer versioned migrations (Flyway or Liquibase) and use a controlled schema-update process.
