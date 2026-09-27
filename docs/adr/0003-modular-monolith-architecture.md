# ADR-0003: Modular monolith architecture

- **Status:** Accepted
- **Date:** 2026-06-22
- **Release / context:** The first release and beyond; the decomposition described here is of the backend domain, and client applications are consumers that take no part in it
- **Related:** [ADR-0002, Additive and driving architectural characteristics](0002-additive-and-driving-architectural-characteristics.md)

## Context

The driving characteristics are reliability under constraint, quota-efficiency, testability and modularity. Load is low (a limited number of readers), the infrastructure is a virtual private server operated by one person, and the architecture is expected to evolve, toward more sports and leagues and a later move off a single server. An early feasibility spike that extracted contexts into separate deployments suffered cross-context boundary leaks. A separate staging host joined the baseline after this decision, which changes none of the reasoning below: the infrastructure stays deliberately small and operated by one person.

## Decision

Build a **domain-partitioned modular monolith**: bounded contexts as top-level **packages**, **hexagonal (ports and adapters) inside each**, with boundaries **enforced** but **not distributed**. The enforcement mechanism is decided in a record of its own. The quota governor and provider machinery run in-process. No context is split into a separately deployable service in the first release.

## Consequences

- Simple operations for one person; no distributed-systems tax.
- The shared quota governor is trivial in-process, against coordinating a shared budget over a network.
- High testability: in-process module calls need no network mocking.
- Clean boundaries preserve **extraction-readiness** as an *additive* option (see [ADR-0002, Additive and driving architectural characteristics](0002-additive-and-driving-architectural-characteristics.md)), so a context, a competition or a league can be split out later when load justifies it.
- Risk: boundary erosion over time, mitigated by enforced architecture tests, not convention.
