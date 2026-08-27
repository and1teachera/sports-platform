# Sports Platform

A spoiler-free sports platform. Browse game results, standings, player statistics and video
highlights without ever being shown a score you didn't ask for. Results are hidden by default
and revealed only when you choose. The first deployment covers the NBA.

> ### Status: pre-development
>
> **This repository currently contains only this README.** There is no application code, and the
> design record is not published here yet. Everything below describes decisions that have been
> made, not software that exists. The first release is targeted for October 2026.
>
> This README documents what is true at each commit, and grows as the repository does.

## The main purpose of this repository

**It is a product.** Anyone who misses a game and plans to watch it later has to avoid the
internet until they do. Scores arrive uninvited through notifications, search results,
thumbnails and headlines. No mainstream service lets you confirm a game happened, watch its
highlights or check the standings without giving the outcome away, so this one does.

**It is an architectural record.** Characteristics were identified and ranked before a style
was chosen. Every significant decision is written down with its context and consequences as it
is made, not reconstructed later. Those records are already written and will be published here
as the repository grows.

## The constraint

The platform runs on free API tiers and costs approximately nothing to operate. That is not a
footnote. It is the force that shaped the architecture. API-Sports allows **100 requests per
day**. ESPN allows **100 per month**. A YouTube search costs 100 of a 10,000-unit daily budget,
while the push notification that replaces it costs zero.

Those numbers made cost-governance a *driving* architectural characteristic rather than an
operational detail, and produced a design that routes every data need to the cheapest viable
provider by data type, freshness requirement and remaining budget; validates every response
against an expected shape before anything is written; and computes internally what no free API
will provide.

## Architecture

A **modular monolith**: one deployable Spring Boot application, bounded contexts as top-level
packages with hexagonal (ports and adapters) layering inside each, and boundaries enforced
mechanically rather than by convention. The architecture is intentionally non-distributed. The
style was derived from the driving characteristics, not chosen first.

The four driving characteristics for v1 are reliability under constraint, quota-efficiency,
testability and modularity. Everything else (security depth, observability depth,
cloud-readiness, extraction-readiness) is classified as *additive*: cheap, reversible
groundwork now so it can be promoted later without a rewrite.

| Layer          | Technology                                    |
| -------------- | --------------------------------------------- |
| Backend        | Java 25, Spring Boot 4.0, Gradle (Kotlin DSL) |
| Database       | PostgreSQL 16, Flyway                         |
| Auth           | Keycloak (OAuth 2.1 + PKCE, OIDC)             |
| Frontend       | Angular, TypeScript, SCSS                     |
| Infrastructure | Docker Compose on a single VPS, Nginx         |

## Open decisions

Two decisions are unresolved, in different ways.

**The authoritative bounded-context set is genuinely open.** The candidate set comes from an
actor/action analysis rather than from entity modelling, and several grouping questions remain
undecided. Publishing a confident module list before that work is finished would be a pretence.
It does not stop work from starting: the first vertical slice is deliberately built inside a
single context, so it does not presuppose the answer.

**The boundary enforcement mechanism is selected but not yet accepted.** A mechanism has been
chosen and documented, but the record stays *proposed* until the first vertical slice exercises
it.
Implementation is part of validating that decision rather than something that waits on it.

## Data sources and legal position

Data will be aggregated from several free-tier providers, each behind an adapter that keeps its
format out of the domain. Highlights will come from YouTube.

The integration is deliberately conservative. Only videos that are **public and
embeddable** will be used, confirmed against the authoritative API rather than assumed. Video
will be shown solely through official embedded players: never re-hosted, never re-uploaded, and
no claim made on any creator's views. Where content is gathered from websites, it will be
gathered only where that site's terms of service permit it. No ownership is claimed over any
provider's data or any creator's video.

## Contributions and support

**This project is not open to contributions.** It is built and maintained by one person, who is
also its sole operator, and it is intended to stay that way. Pull requests will not be
accepted. The repository is public so the work can be read, not so it can be developed
collectively.

## License

Copyright (c) 2026 Angel Zlatenov. All rights reserved.

The code and documentation here are published to be read, not reused. No permission is granted
to copy, modify or redistribute them. This is a deliberate choice rather than an oversight: the
platform is intended to run as a live service maintained by its author.
