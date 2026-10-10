# Personal Finance Manager API

A REST API where each user can record income and expenses, organise them by category, track savings goals, and see monthly and yearly reports. Every user only sees their own data.

Built with Java 17, Spring Boot 3, Maven, Spring Data JPA, Spring Security (session login), Bean Validation and H2. The request and response formats follow the assignment PDF.

## 1. What you need

- Java 17 or newer
- Maven 3.9 or newer

## 2. Run it on your computer

    mvn spring-boot:run

The API starts at http://localhost:8080 with the "local" profile: an in-memory H2 database (data is lost when you stop the app) and the H2 console at http://localhost:8080/h2-console (JDBC URL jdbc:h2:mem:financedb, user sa, empty password).

Health check: http://localhost:8080/actuator/health

## 3. Run the tests and see coverage

    mvn clean verify

- Runs the unit tests and the API tests.
- Writes the JaCoCo report to target/site/jacoco/index.html.
- The build fails if line coverage is below 80 percent.

## 4. How login works

1. POST /api/auth/register creates an account.
2. POST /api/auth/login checks the password and returns a session cookie (JSESSIONID). Send this cookie on every later request. Thunder Client, Postman and curl (with a cookie jar) do this automatically.
3. POST /api/auth/logout ends the session.

All routes except register and login need a valid session and return 401 otherwise.

Example with curl:

    curl -c jar.txt -H "Content-Type: application/json" -d '{"username":"user@example.com","password":"password123","fullName":"John Doe","phoneNumber":"+1234567890"}' http://localhost:8080/api/auth/register
    curl -c jar.txt -b jar.txt -H "Content-Type: application/json" -d '{"username":"user@example.com","password":"password123"}' http://localhost:8080/api/auth/login
    curl -b jar.txt -H "Content-Type: application/json" -d '{"amount":50000.00,"date":"2024-01-15","category":"Salary","description":"January Salary"}' http://localhost:8080/api/transactions

The PDF asks for session cookies only, so CSRF protection is off by default. It can be switched on with APP_SECURITY_CSRF_ENABLED=true (clients then send the X-XSRF-TOKEN header on POST, PUT and DELETE).

## 5. Endpoints

Full details with examples: docs/api.md

- Auth: POST /api/auth/register, POST /api/auth/login, POST /api/auth/logout
- Transactions: POST, GET (startDate, endDate, categoryId, type), PUT /{id}, DELETE /{id} under /api/transactions
- Categories: GET /api/categories, POST /api/categories, DELETE /api/categories/{name}
- Goals: POST, GET, GET /{id}, PUT /{id}, DELETE /{id} under /api/goals
- Reports: GET /api/reports/monthly/{year}/{month}, GET /api/reports/yearly/{year}

Error format (every error):

    {
      "timestamp": "2026-10-10T06:00:00Z",
      "status": 400,
      "error": "Bad Request",
      "code": "VALIDATION_ERROR",
      "message": "Request validation failed",
      "fieldErrors": [ { "field": "amount", "message": "must be greater than 0" } ],
      "path": "/api/transactions"
    }

## 6. Design decisions

- Layers: Controller, Service, Repository. DTOs are separate from entities. One global exception handler (@RestControllerAdvice).
- Money uses BigDecimal with 2 decimals. Goal percentages are rounded to 2 decimals (HALF_UP).
- Goal progress and reports are calculated from current transactions every time, so edits and deletes are reflected at once.
- The transaction type comes from the category you choose. Categories are sent by name.
- Category names are unique per user and cannot equal a default name (ignoring case). This keeps name lookups unambiguous.
- Status codes follow the PDF: deleting a default category is 403; deleting a category used by a transaction is 400; another user's goal is 403; another user's transaction is 404 (the PDF does not list 403 for transactions).
- Goal target date must be in the future. Goal start date defaults to today and cannot be in the future.
- Business timezone is Asia/Kolkata (app.timezone) and currency is INR (app.currency). "Today" and the "no future dates" rule use this timezone.
- Passwords are stored with BCrypt. The session cookie is HttpOnly (Secure on HTTPS in prod). CSRF is off by default because the PDF does not ask for it.

## 7. Deploying on Render

The H2 database lives in memory, so data is lost when the app restarts. The assignment lists H2 as optional, so this is acceptable for the demo.

1. Push the project to a public GitHub repository.
2. On Render choose New, Web Service, pick the repository and choose the Docker runtime (a Dockerfile is included).
3. Set the health check path to /actuator/health and deploy.
4. Run: bash financial_manager_tests.sh https://YOUR-APP.onrender.com/api
5. Email the repository link, the deployed URL and a screenshot of the test report.

The prod profile turns off the H2 console and marks the session cookie Secure, so login only works over https there.

## 8. Project layout

    src/main/java/com/example/finance
      config/       settings, security setup, default category seeding
      controller/   HTTP routes only
      service/      business rules
      repository/   database access (always scoped to the logged-in user)
      entity/       database tables
      dto/          request and response shapes
      security/     login helpers
      exception/    error handling
    src/test/java   unit tests (service) and API tests (integration)
    docs/           the original specification documents (docs/api.md is the current API)

## 9. Settings

- SPRING_PROFILES_ACTIVE: local (default), test or prod
- SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD: database (prod)
- APP_SECURITY_CSRF_ENABLED: false by default
- APP_TIMEZONE, APP_CURRENCY: business settings
- PORT: HTTP port in prod
