# Technical decisions

> ### Status
>
> This document is the technical baseline that implementation must respect: the technology,
> protocol and deployment choices already made. No application code exists yet; each entry states a
> decision in force, and the software that follows it does not exist. The document says what
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
- **Metrics collection and dashboards deferred.** A metrics collector and a dashboard stack are not part of the first release, and each returns with its own decision record when the need is real, as [ADR-0002, Additive and driving architectural characteristics](adr/0002-additive-and-driving-architectural-characteristics.md) sets out. The backend exposes health and metrics endpoints with a metrics registry from the start; that endpoint is the seam that keeps the deferral a configuration change rather than a rewrite.

## Deployment composition

- **Containers.** The system deploys as five Docker containers under Docker Compose: the Spring Boot application, PostgreSQL 16, Keycloak, the public application's server runtime, and Nginx, which serves the administrative application's static files, reverse proxies the API and the public application, and terminates TLS. The [architecture overview](architecture.md) shows how they relate.
- **Hosts.** A production host and a staging host, each running the same five containers under Docker Compose, deliberately with different hosting companies. Staging verifies correctness, not capacity.
