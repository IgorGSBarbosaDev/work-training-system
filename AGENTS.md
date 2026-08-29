# Repository Guide

## Sources Of Truth

- `work-training-system-fonte-da-verdade.md` is the official MVP scope. Read the relevant sections before domain work; scope changes must be recorded there before implementation.
- All PRDs and supporting product documents must be stored in `docs/`. Read the relevant files in this directory before planning or implementing a feature.
- Treat the frontend, Docker Compose, CI, object storage, and the domain modules in that document as the planned architecture; verify their current implementation in the repository before making decisions.
- For runnable behavior and dependency versions, trust `pom.xml` and `src/` over the scope document or generated `HELP.md`.

## Stack

- Backend: Java 21, Spring Boot and Maven.
- API: Spring Web and Bean Validation.
- Security: Spring Security, OAuth2 Resource Server and JWT.
- Persistence: Spring Data JPA, PostgreSQL and Flyway.
- Development: Lombok and Spring Boot DevTools.
- Monitoring: Spring Boot Actuator.
- Tests: JUnit, Mockito and Testcontainers.
- Frontend: React, TypeScript and Vite.
- Infrastructure: Docker Compose, object storage and GitHub Actions.

## Commands

- Use the checked-in wrapper and Java 21 or newer: `./mvnw ...` (`pom.xml` compiles with `release 21`, which is the minimum compatibility target).
- Compile and package without starting containers: `./mvnw -DskipTests package`. This still compiles test sources.
- Run all tests: `./mvnw test`.
- Run one test class or method: `./mvnw -Dtest=WorkTrainingSystemApplicationTests test` or `./mvnw -Dtest=WorkTrainingSystemApplicationTests#contextLoads test`.
- Run the development application with its managed database: `./mvnw spring-boot:test-run`. This selects `TestWorkTrainingSystemApplication`, which adds the Testcontainers configuration.

## Runtime And Tests

- A working Docker-compatible daemon is required by the current context test and `spring-boot:test-run`; `TestcontainersConfiguration` starts `postgres:latest` and supplies connection details via `@ServiceConnection`.
- `src/main/resources/application.yaml` has no datasource configuration. Plain `./mvnw spring-boot:run` therefore needs external datasource settings; use `spring-boot:test-run` for the self-contained development path.
- Import `TestcontainersConfiguration` in full-context tests that need PostgreSQL, following `WorkTrainingSystemApplicationTests`.
- There is currently no configured formatter or static-analysis task. Frontend lint/build and the CI workflow are available in addition to Maven compilation and tests.

## Documentation Of Changes

- After every modification, implementation or correction, document in simple, explanatory language what changed, why it changed, the affected area and the validations executed.
- Update the relevant Markdown files in `docs/` to record what was implemented, what remains pending and any API, route or operational contract changes.
- Update `work-training-system-fonte-da-verdade.md` before implementing any change to the MVP scope.

## Code Quality

- Follow Clean Code, KISS, DRY and YAGNI. Use clear names, small methods and single-purpose classes.
- Keep controllers thin, business rules in services/domain components, and never expose JPA entities directly through the API.
- Prefer constructor injection, immutable objects and DTOs with Bean Validation.
- Use Lombok only to reduce boilerplate; avoid `@Data` on JPA entities.
- Centralize exception handling and never expose sensitive data in responses or logs.
- Make retryable operations idempotent and protect against duplicates with database constraints.
- Use transactions in application services and avoid external operations inside database transactions.
- Use pagination, appropriate indexes and explicit JPA fetching to prevent unnecessary queries and N+1 problems.
- Add unit tests for business rules and integration tests for persistence, security and critical workflows.
- Every change must compile, pass relevant tests and remain consistent with the documented MVP scope.

## Code Shape

- Production component scanning starts at `dev.igorbarbosa.worktrainingsystem.WorkTrainingSystemApplication`; keep production packages beneath that root.
- Preserve the modular-monolith direction in section 23 of the scope document when introducing domain packages; do not split the planned modules into services without first changing the scope.
- Keep dependencies between modules explicit and avoid circular dependencies.
- Do not access another module's repository directly; interact through its public application interface.
- New code must compile, pass relevant tests and remain consistent with the documented MVP scope.
