# Technical decisions

> ### Status
>
> This document is the technical baseline that implementation must respect: the technology,
> protocol and deployment choices already made. The repository holds the build baseline below and a
> backend with none of the behaviour the [requirements](requirements.md) describe. Of the other
> entries, the deny-by-default authorization rule, structured logging with correlation IDs and the
> health endpoint under observability are in place; every remaining entry states a decision in
> force whose software does not exist yet. The document says what
> applies; the reason for an architecturally significant choice is in its decision record, linked
> where one is published. Decisions that concern data collection, resilience, operations or a
> particular feature are published with the work that needs them.

## Runtime and persistence

- **Backend.** One Spring Boot application holds the whole backend: every bounded context runs in that one process, as [ADR-0003, Modular monolith architecture](adr/0003-modular-monolith-architecture.md) decides.
- **Database.** PostgreSQL 16.
- **Client applications.** The public application is written in Next.js with React and TypeScript and renders on the server from API responses. The administrative application is written in Angular with TypeScript and is served as a static build. Both are clients of the same API, as [ADR-0004, Two client applications, chosen against their own constraints](adr/0004-client-architecture.md) decides. Component libraries and styling tooling are implementation choices, made when the clients are built.

## Identity and access

- **Authentication.** The system uses OAuth 2.1 with PKCE via Keycloak for user authentication. Registration, login, password reset, user sessions and the token lifecycle belong to Keycloak, which is the external identity service outside the system boundary; the system only recognises a logged-in user.
- **Token validation.** The backend validates the JWT tokens Keycloak issues on all protected endpoints. The system has two roles, USER and ADMIN, as the [requirements](requirements.md#role-based-access) state.
- **Authorization.** Deny-by-default: an endpoint with no explicit rule is refused.
- **TLS.** Nginx provides TLS termination.

## API conventions

- **Response format.** Successful API responses use a standard envelope format, and errors use RFC 9457 Problem Details. Error responses carry custom error codes and locale-aware messages, so that what a reader sees when something fails is in the language they chose and carries no internals.

## Observability

- **Structured logging.** Application logging is structured JSON with correlation IDs, from the start.
- **Metrics collection and dashboards deferred.** A metrics collector and a dashboard stack are not part of the first release, and each returns with its own decision record when the need is real, as [ADR-0002, Additive and driving architectural characteristics](adr/0002-additive-and-driving-architectural-characteristics.md) sets out. The backend has a metrics registry from the start. Its health endpoint is open to anyone, and its metrics and info endpoints are denied to anonymous callers until a concrete consumer needs them; the metrics endpoint is the seam that keeps the deferral a configuration change rather than a rewrite.

## Deployment composition

- **Containers.** The system deploys as five Docker containers under Docker Compose: the Spring Boot application, PostgreSQL 16, Keycloak, the public application's server runtime, and Nginx, which serves the administrative application's static files, reverse proxies the API and the public application, and terminates TLS. The [architecture overview](architecture.md) shows how they relate.
- **Hosts.** A production host and a staging host, each running the same five containers under Docker Compose, deliberately with different hosting companies. Staging verifies correctness, not capacity.

## Build baseline

- **Language.** Java 25, set as the Gradle toolchain: every build and every run uses the same runtime.
- **Application framework.** Spring Boot 4. The exact release is set in the build files.
- **Build tool and layout.** Gradle with the Kotlin DSL: one build at the root of the repository, in which the backend is one Gradle module, `backend`. The Gradle wrapper is committed, so the build runs with `./gradlew` and needs no Gradle installation.
- **Database migrations.** Flyway, as versioned SQL migrations applied at application startup. Spring Boot runs Flyway as part of its own startup, and the backend includes Flyway's PostgreSQL support. The first migration creates the tables of the League structure, and Flyway applies it, like every later one, at startup.
- **Tests.** JUnit Jupiter, as Spring Boot's test starter brings it. Tests that need a database run against PostgreSQL 16 in Testcontainers, which needs a container runtime on the machine that runs them. Testcontainers is taken from the Spring Boot dependency management and is not versioned separately. A smoke test starts the whole application context against such a database and checks that Flyway is wired into the startup. Integration tests share one PostgreSQL container across test classes. The build and its tests run on every pull request, and the build includes architecture tests, written with ArchUnit, that fail it when the code breaks a boundary rule, as [ADR-0006, Boundary enforcement](adr/0006-boundary-enforcement.md) decides.
