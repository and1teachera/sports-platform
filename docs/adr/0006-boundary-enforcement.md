# ADR-0006: Boundary enforcement

- **Status:** Accepted
- **Date:** 2026-10-01
- **Release / context:** The first release; the mechanism half of the modular monolith decision
- **Related:** [ADR-0002, Additive and driving architectural characteristics](0002-additive-and-driving-architectural-characteristics.md) (the additive guardrail); [ADR-0003, Modular monolith architecture](0003-modular-monolith-architecture.md) (requires enforcement and leaves the mechanism open); [ADR-0005, Bounded contexts](0005-bounded-contexts.md) (the contexts these rules protect)

## Context

[ADR-0003, Modular monolith architecture](0003-modular-monolith-architecture.md) requires the boundaries between contexts, and the layering inside each, to be enforced mechanically rather than by convention, and leaves open how. Several mechanisms are available: package conventions, architecture tests, Gradle modules, the Java module system and Spring Modulith. They work at different granularities. Gradle modules and the Java module system decide which module may depend on which. Neither can express the rules that carry the most weight here: which code may reach a provider host, and what a response may carry, so that no outcome-bearing information reaches a reader who did not ask for it. Only architecture tests can.

Two facts decide the mechanism. [ADR-0005, Bounded contexts](0005-bounded-contexts.md) decides the contexts the first features need and leaves the rest of the set open, so a mechanism that encodes the whole set would commit the system to more than it has decided. And the code is young, so the aim is to avoid irreversible mistakes, not to enforce as much as possible.

## Decision

**1. Architecture tests over a package-by-context structure.** Bounded contexts are top-level packages, hexagonal inside each, as [ADR-0003, Modular monolith architecture](0003-modular-monolith-architecture.md) decides, and [ADR-0005, Bounded contexts](0005-bounded-contexts.md) names the contexts. The rules are architecture tests, written with ArchUnit, that run in the build and fail it on a violation, as a required check on every pull request. A rule is code in a test, so re-cutting a context stays cheap as long as the set of contexts is open.

**2. The package structure is the commitment this record makes.** Package-by-context with layers inside it is the one irreversible choice here, and everything else bolts onto it. What another context consumes stays reachable at its context's package root and not nested, and what is published and what is not are tests in the build. They name League's packages because League is the only context; a further context adopts the same rules for its own packages when its code exists.

**3. Rules that enforce the first-release boundaries.**

| Rule | What the build checks |
|---|---|
| Domain independence | League's domain depends only on itself and the JDK |
| No framework in the domain | The domain uses none of Spring, Lombok, Jackson, Hibernate or Flyway |
| No hidden clock | The domain never asks the system for the current time |
| No input or output | The domain uses nothing from `java.io`, `java.nio.file`, `java.net` or `java.sql` |
| No provider reference | The domain declares no type that represents a provider reference |
| Application layer | League's application layer depends neither on the web edge nor on infrastructure, and not on Spring's web, JDBC or transaction packages |
| Persistence adapters | League's persistence adapters do not depend on the web edge |
| HTTP clients | No class outside provider acquisition depends on an HTTP client, `java.net.http` included |
| Isolation | While League is the only context, isolation is enforced at League's domain, which the independence rule confines to itself and the JDK |

The first five rules keep the domain pure: it has no framework, no input or output, no hidden clock, and nothing that carries a provider reference, which ADR-0005 forbids in every domain model. The next two keep dependencies pointing inward, toward the domain. The HTTP-client rule keeps provider traffic in one place: only the capability that governs the provider allowances reaches a provider host.

The rules that hold whole contexts apart name packages that do not exist yet, so they are written with the second context: that League depends on no other context, that Games has no dependency on Accounts, and that a context reaches another only through what the other publishes at the root of its package. Rules for code that does not exist are not written yet. Likewise, rules for Games and Accounts, for responses scoped to one reader and for listings, are written with the code that creates their subject.

**4. Spring Modulith is deferred.** Its verifier derives modules from the package tree, so running it ratifies a set of contexts. That ratification belongs to the record that decides the contexts, ADR-0005, and not to a tool's default. Whether to adopt it is evaluated against that set, including whether its export model behaves as rule 2 assumes.

**5. Gradle modules are deferred.** They would enforce at compile time a set of contexts that is decided only in part, and charge for it on every boundary change. Under the guardrail of [ADR-0002, Additive and driving architectural characteristics](0002-additive-and-driving-architectural-characteristics.md), they are not additive: they are structure and not a cheap, reversible seam, and the characteristic they would serve, extraction-readiness, is additive rather than driving. They are revisited when a context is actually extracted.

**6. The Java module system is declined.** Spring Boot's executable jar is incompatible with the module path, so adopting it would cost the single-artifact deployment the operating model rests on. Spring, Jackson and the test tooling all need reflective access, so the encapsulation would be handed back through `opens`. It duplicates what Gradle modules already offer and expresses none of the semantic rules. It is a tool for publishing libraries, and the system publishes none.

## Consequences

- Enforcement starts with the first layered code instead of waiting for the full set of contexts, because most of the rules do not depend on it.
- The known weakness is that a test can be disabled. It is mitigated by making the architecture tests part of the build every pull request must pass, not by a heavier mechanism.
- The options on Spring Modulith and Gradle modules stay open, at no cost to keep.
- Not every rule has code to check on the day it is written. When these rules were written, the layers they name that had code were League's domain and its persistence adapters. The five domain rules check League's domain. The persistence rule checks the persistence adapters against a web edge that does not exist yet. The HTTP-client rule applies to every class, and because provider acquisition has no code it forbids an HTTP client everywhere. The application-layer rule constrains a layer that does not exist yet, and the isolation rule holds because the independence rule holds. A rule whose layer has no code constrains the first class written in that layer; nothing here enforces anything for Games or Accounts, which have no code.
- The cost claim is checked by writing the rules and running them in the build. If any rule proves materially more expensive than this record assumes, a new record supersedes this one, as [ADR-0001, Record architecture decisions](0001-record-architecture-decisions.md) describes.
- Nothing here decides the contexts. [ADR-0005, Bounded contexts](0005-bounded-contexts.md) does, and this record says how the boundaries it draws are held.
