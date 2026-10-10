# Personal Finance Manager API

A REST API where each user can record income and expenses, organise them by category, track savings goals, and see monthly and yearly reports. Every user only ever sees their own data.

Built with Java 17, Spring Boot 3, Maven, Spring Data JPA, Spring Security (session login), Bean Validation and H2.

## 1. What you need

- Java 17 or newer
- Maven 3.9 or newer (the Maven wrapper is not included)

## 2. Run it on your computer

Open a terminal in this folder and run:

    mvn spring-boot:run

The API starts at http://localhost:8080. It uses the "local" profile by default: an in-memory H2 database (data is lost when you stop the app) and the H2 console at http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:financedb, user: sa, empty password).

Health check: GET http://localhost:8080/actuator/health

## 3. Run the tests and see coverage

    mvn clean verify

- Runs all unit tests and API integration tests.
- Writes the JaCoCo report to target/site/jacoco/index.html.
- The build fails if line coverage drops below 80 percent.

To only run tests: mvn test

## 4. How login works (important for API clients)

1. POST /api/auth/register creates an account.
2. POST /api/auth/login checks the password and returns a session cookie (JSESSIONID). Send this cookie on every later request.
3. For POST, PUT and DELETE requests, CSRF protection is on. The server sends a cookie named XSRF-TOKEN. Copy its value into a header named X-XSRF-TOKEN on every POST, PUT and DELETE request. A simple way to get the cookie is to call GET /api/auth/me after login.
4. Register and login do not need the CSRF header. Logout does.
5. POST /api/auth/logout ends the session.

Example with curl (cookies saved in a file):

    curl -c jar.txt -H "Content-Type: application/json" -d '{"fullName":"Alex","email":"alex@example.com","password":"a-long-password"}' http://localhost:8080/api/auth/register
    curl -c jar.txt -b jar.txt -H "Content-Type: application/json" -d '{"email":"alex@example.com","password":"a-long-password"}' http://localhost:8080/api/auth/login
    curl -c jar.txt -b jar.txt http://localhost:8080/api/auth/me
    TOKEN=$(grep XSRF-TOKEN jar.txt | awk '{print $7}')
    curl -b jar.txt -H "X-XSRF-TOKEN: $TOKEN" -H "Content-Type: application/json" -d '{"amount":125.50,"date":"2026-10-05","categoryId":2,"description":"Groceries","type":"EXPENSE"}' http://localhost:8080/api/transactions

If an assignment test script cannot send a CSRF token, you can switch CSRF off for that run only with the setting app.security.csrf-enabled=false (or the environment variable APP_SECURITY_CSRF_ENABLED=false). Keep it on otherwise.

## 5. Endpoints

Authentication
- POST /api/auth/register - 201, 400 (invalid), 409 (email already used)
- POST /api/auth/login - 200 with user summary, 401 (wrong email or password)
- POST /api/auth/logout - 204
- GET /api/auth/me - 200, 401

Transactions (login required)
- POST /api/transactions - 201
- GET /api/transactions - optional filters: from, to (YYYY-MM-DD, inclusive), categoryId, type (INCOME or EXPENSE). Newest first.
- PUT /api/transactions/{id} - 200. The date cannot be changed (send the same date or leave it out).
- DELETE /api/transactions/{id} - 204

Categories (login required)
- GET /api/categories - default categories plus your own
- POST /api/categories - body: name, type. 201, or 409 if the name already exists
- DELETE /api/categories/{name} - 204. Add ?type=INCOME or ?type=EXPENSE only if you have two categories with the same name.

Savings goals (login required)
- POST /api/goals, GET /api/goals, GET /api/goals/{id}, PUT /api/goals/{id}, DELETE /api/goals/{id}
- Responses include progress, completionPercentage and remaining, calculated live from your transactions.

Reports (login required)
- GET /api/reports/monthly/{year}/{month}
- GET /api/reports/yearly/{year}

Full details: docs/api.md

## 6. Error format

Every error uses the same JSON shape:

    {
      "timestamp": "2026-10-10T06:00:00Z",
      "status": 400,
      "error": "Bad Request",
      "code": "VALIDATION_ERROR",
      "message": "Request validation failed",
      "fieldErrors": [ { "field": "amount", "message": "must be greater than 0" } ],
      "path": "/api/transactions"
    }

Status codes: 400 invalid input, 401 not logged in, 403 forbidden (also a missing CSRF token), 404 not found or not yours, 409 conflict, 500 unexpected error (no stack trace is sent).

## 7. Decisions made where the specs were open

- Currency: INR (app.currency). No currency conversion.
- Business timezone: Asia/Kolkata (app.timezone). "Today" and the "no future dates" rule use it.
- Money has 2 decimal places. More than 2 decimals is rejected.
- Completion percentage is rounded to 2 decimals (HALF_UP). Progress and remaining are never clamped, so they can be negative or above the target.
- Goal progress counts your income minus expenses from the goal start date up to today (inclusive).
- Deleting a default category returns 403. Deleting someone else's or a missing category returns 404. Deleting a category used by a transaction returns 409.
- Goal update (PUT) needs targetAmount and targetDate. goalName is optional. The start date cannot be changed.
- Logout always returns 204, even if you were not logged in.
- Category names with a slash (/) cannot be deleted by name because of the URL path.
- The exact JSON shapes of the original assignment PDF and test script were not supplied. Compare them with docs/api.md before submitting and adjust field names if they differ.

## 8. Deploying (for example on Render)

The H2 database lives in memory, so all data is lost whenever the app restarts. If the assignment needs data to survive restarts, use a hosted database: add its JDBC driver to pom.xml (for example PostgreSQL) and set the environment variables below.

Render runs Java through Docker, so a Dockerfile is included.

1. Push this project to a public GitHub repository.
2. On Render, create a new Web Service from the repository and choose the Docker runtime.
3. Set these environment variables:
   - SPRING_PROFILES_ACTIVE = prod (already set inside the Dockerfile)
   - SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD (only if you use a hosted database)
   - Render provides PORT automatically.
4. Deploy and check /actuator/health.
5. Register, log in, create a transaction, view a report, then run the assignment test script against your public URL.
6. Save the repository URL, the deployed URL and a screenshot of the test report.

The prod profile turns off the H2 console, marks the session cookie as Secure (HTTPS only) and trusts the host's forwarded headers. Because the cookie is Secure, login will not work over plain http in the prod profile.

## 9. Project layout

    src/main/java/com/example/finance
      config/       app settings, security setup, default category seeding
      controller/   HTTP routes only
      service/      business rules
      repository/   database access (always scoped to the logged-in user)
      entity/       database tables
      dto/          request and response shapes
      security/     login helpers
      exception/    error handling
    src/test/java   unit tests (service) and API tests (integration)
    docs/           the original specification documents

## 10. Configuration reference

- SPRING_PROFILES_ACTIVE: local (default), test or prod
- SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD: database
- SPRING_JPA_HIBERNATE_DDL_AUTO: update by default; use proper migrations (Flyway or Liquibase) for a real production database
- SPRING_H2_CONSOLE_ENABLED: true only in local
- APP_TIMEZONE, APP_CURRENCY: business settings
- APP_SECURITY_CSRF_ENABLED: true by default
- PORT: HTTP port in prod
