# Environment and Configuration
## Personal Finance Manager

## 1. Purpose
Keep environment-specific settings outside source code. Never commit real credentials, session secrets, or production connection details.

## 2. Local H2 configuration
The roadmap’s local configuration is:

```properties
spring.datasource.url=jdbc:h2:mem:financedb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Keep the H2 console enabled only in a local development profile. The in-memory database resets when the application stops.

## 3. Recommended profiles
- `local`: H2 in-memory, H2 console enabled, developer-friendly diagnostics.
- `test`: isolated test database/configuration, deterministic test clock and fixtures.
- `prod`: durable database if required, H2 console disabled, secure cookies, externalized credentials, reduced error detail.

Avoid duplicating large configuration blocks. Use profile-specific files such as `application-local.properties`, `application-test.properties`, and `application-prod.properties`.

## 4. Configuration inventory
| Setting | Purpose | Local default | Production guidance |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` | Set explicitly |
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:h2:mem:financedb` | Use host-supported durable DB if persistence is required |
| `SPRING_DATASOURCE_USERNAME` | DB username | `sa` | Secret/config store |
| `SPRING_DATASOURCE_PASSWORD` | DB password | blank for local H2 | Secret manager; never commit |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Schema behavior | `update` | Prefer controlled migrations; avoid blind production updates |
| `SPRING_H2_CONSOLE_ENABLED` | H2 console toggle | `true` locally | `false` |
| `SERVER_SERVLET_SESSION_COOKIE_HTTP_ONLY` | Prevent script access to session cookie | `true` | `true` |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | HTTPS-only cookie | `false` on local HTTP | `true` on HTTPS |
| `SERVER_SERVLET_SESSION_COOKIE_SAME_SITE` | Cross-site cookie policy | Appropriate local value | Usually `Lax`; validate client/deployment needs |
| `SERVER_PORT` | HTTP port | Framework default | Use host-provided port where required |

Exact environment-variable binding depends on the Spring Boot version and property naming. Verify property support against the selected Spring Boot 3 minor release.

## 5. Secrets
- Do not store production credentials in `application.properties`.
- Use host environment variables or a secret manager.
- Keep `.env` files out of version control; commit a `.env.example` with placeholders only if a dotenv workflow is actually used.
- Rotate exposed credentials immediately.
- Never log environment dumps in production.

## 6. Startup checks
Before running:
1. Confirm Java 17 is active.
2. Confirm Maven wrapper or compatible Maven is available.
3. Select the intended Spring profile.
4. Confirm database URL and credentials.
5. Confirm production H2 console is disabled.
6. Run tests before packaging.

## 7. Persistence warning
`jdbc:h2:mem:financedb` is ephemeral. It is suitable for local development and automated tests, but data is lost when the JVM stops. Before deployment, confirm whether the assignment expects data to survive restarts. If yes, select a durable database supported by the host and configure it through environment variables.
