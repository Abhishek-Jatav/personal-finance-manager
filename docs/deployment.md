# Deployment Guide
## Personal Finance Manager

## 1. Deployment goals
Publish a runnable REST API, document its URL, and run the assignment-provided test script against the deployed service. Render or another suitable host may be used, subject to current Java/runtime and database support.

## 2. Pre-deployment checklist
- All required endpoints implemented and tested.
- At least 80% code coverage measured with JaCoCo.
- README contains setup, endpoint, authentication, and test instructions.
- No credentials or local database files committed.
- Production profile disables H2 console.
- Error responses do not reveal stack traces.
- Session cookie and CSRF behavior are tested over HTTPS.
- Persistence expectations are confirmed.
- Assignment test script and expected environment variables are available.

## 3. Build
Use the Maven wrapper if included:
```bash
./mvnw clean test package
```
On Windows:
```powershell
mvnw.cmd clean test package
```
If there is no wrapper, use a compatible Maven installation:
```bash
mvn clean test package
```
The runnable JAR is normally created under `target/`.

## 4. Hosting configuration
1. Create a new web service from the public repository.
2. Select a Java 17-compatible runtime/build environment.
3. Configure the build command to run tests and package the application.
4. Configure the start command to run the generated JAR, or use the host’s supported Java service configuration.
5. Set the active profile to `prod`.
6. Configure a port binding supported by the host.
7. Set database environment variables and secrets if a durable database is used.
8. Ensure HTTPS is enabled and forwarded-header handling is configured correctly if the host terminates TLS.
9. Deploy and inspect logs for startup failures.

Do not assume every host provides a persistent filesystem or managed SQL database on every plan. Verify current host capabilities before committing to a persistence design.

## 5. Database and persistence
The roadmap’s H2 in-memory URL loses all data on application stop. This may be acceptable for a short-lived demo only if the assignment permits it. If data must persist, configure a durable database supported by the host. Validate schema creation/migrations and connection pooling in the deployment environment.

## 6. Post-deployment verification
- Check health/startup endpoint if configured.
- Register a test user.
- Log in and verify session cookie behavior.
- Obtain and send CSRF tokens for state-changing requests when required.
- Create/list/update/delete a transaction.
- Verify user-scoped access using a second account.
- Verify category and goal endpoints.
- Verify monthly/yearly reports.
- Confirm logout invalidates the session.
- Run the provided assignment test script against the public API URL.
- Save the repository URL, deployment URL, and screenshot of the test report.

Use non-sensitive test data only.

## 7. Rollback and troubleshooting
- **Build fails:** inspect Java/Maven versions and dependency resolution.
- **Application fails to start:** inspect environment variables, port binding, and database connectivity.
- **Login works locally but not deployed:** inspect cookie Secure/SameSite settings, proxy HTTPS handling, and CSRF configuration.
- **Data disappears:** verify whether the configured database is in-memory or the host filesystem is ephemeral.
- **Unexpected 403:** check CSRF token flow and security route rules.
- **Unexpected 404 on owned data:** inspect current-user resolution and owner-scoped repository predicates.
- **Database schema mismatch:** use controlled migrations or reset only disposable development data.

## 8. Submission package
Submit:
- Public repository URL.
- Deployed API URL.
- README/setup instructions.
- Test report screenshot.
- Assignment test-script result.
- Coverage report or documented command/output.
