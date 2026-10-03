# Sports Platform

A spoiler-free sports platform. Its purpose is to let readers access information about sports events without revealing the game result unless they choose to see it. Most sports services deliver game results as quickly as possible, through notifications, headlines, search matches, video thumbnails and social media feeds, and give people no direct control over when an outcome is revealed; people who watch a game later than it was played need the opposite. A game result will be shown only when the reader explicitly asks for it, and the same applies to anything that could indirectly reveal the outcome: partial scores, player statistics, changes in the standings, or images that show how the event ended. The first version will focus on one sport and one league: basketball and the NBA.

> ### Status: build skeleton
>
> The Gradle build and the Spring Boot backend module compile from a clean clone, and one smoke test proves the Spring context starts and Flyway reaches the database. There is no application behaviour yet; later work adds it.

## Constraints

Three constraints apply to every part of the system and to every decision made about it. The [system description](docs/system-description.md) states them in full.

- **Data costs nothing; hosting is a small fixed cost.** The system operates within the free allowances that external data providers and the video platform offer, and a paid data source is used only by explicit decision. Those allowances are small and fixed, so how often and how much the system requests from them is limited by the allowances, not by reader demand.
- **Use of media is legally clean.** The system stores a video only if its platform marks it as public and embeddable when it is stored, shows it through that platform's own player, never re-hosts or re-uploads it, and claims no ownership over any provider's data or any creator's video.
- **One person builds and operates the system.** There is no separate operations role and no second person. Every operational choice must be one that a single person can run alongside building the system.

## Architecture and technology

The design is a domain-partitioned modular monolith: bounded contexts as top-level packages, hexagonal (ports and adapters) inside each, with boundaries enforced but not distributed. No context is split into a separately deployable service in the first release. Which bounded contexts the application will hold is not yet decided.

Two client applications will serve the two interactive surfaces, each chosen against its own surface: a public application, reachable without signing in, whose pages render and are cacheable with no session attached to them, and an administrative application that serves the Administrator, one person, behind a login.

| Part | Technology | In the repository |
|---|---|---|
| Backend | Java 25 with Spring Boot 4, built with Gradle | Yes, as a build skeleton: the application context starts and Flyway runs in its startup; no application behaviour yet |
| Database | PostgreSQL | In tests only: PostgreSQL 16 in a container that Testcontainers starts; Flyway runs against it, with no migrations yet |
| Identity | Keycloak, with OAuth 2.1 and PKCE | Not yet |
| Public application | Next.js with React and TypeScript | Not yet |
| Administrative application | Angular with TypeScript | Not yet |
| Deployment | Docker Compose, with Nginx in front | Not yet |

The [architecture overview](docs/architecture.md) describes the shape and its containers in full, the [technical decisions](docs/technical-decisions.md) hold the baseline implementation must respect, and two decision records explain the choices: [ADR-0003](docs/adr/0003-modular-monolith-architecture.md) for the modular monolith and [ADR-0004](docs/adr/0004-client-architecture.md) for the two client applications.

## Documentation

- [System description](docs/system-description.md): the problem the system addresses, what it is meant to accomplish, the constraints that shape it, who and what interacts with it, where its boundary lies, what it excludes, and the scope of the first version
- [Phase One scope](docs/phase-one-scope.md): what the first version delivers and what it deliberately leaves out
- [Requirements](docs/requirements.md): the foundational behaviour the published specification requires, extended as the work that implements more of it is published
- [Technical decisions](docs/technical-decisions.md): the technical baseline that implementation must respect
- [Architecture overview](docs/architecture.md): how the system is shaped, its context and its containers
- [Glossary](docs/glossary.md): the meaning each term carries across these documents, and the requirement that owns it where one does
- Architecture decision records, why the foundational choices were made and what they cost. Each record is published with the work it governs.
  - [ADR-0001: Record architecture decisions](docs/adr/0001-record-architecture-decisions.md)
  - [ADR-0002: Additive and driving architectural characteristics](docs/adr/0002-additive-and-driving-architectural-characteristics.md)
  - [ADR-0003: Modular monolith architecture](docs/adr/0003-modular-monolith-architecture.md)
  - [ADR-0004: Two client applications, chosen against their own constraints](docs/adr/0004-client-architecture.md)

Further documentation is published with the work that needs it and listed here as it lands.

## Contributions and support

**This project is not open to contributions.** It is built and maintained by one person, who is
also its sole operator, and it is intended to stay that way. External pull requests are not
accepted. The repository is public so the work can be read, not so it can be developed
collectively.

## License

Copyright (c) 2026 Angel Zlatenov. All rights reserved.

The code and documentation here are published to be read, not reused. No permission is granted
to copy, modify or redistribute them. This is a deliberate choice rather than an oversight: the
platform is intended to run as a live service maintained by its author.
