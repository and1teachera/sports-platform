# ADR-0002: Additive and driving architectural characteristics

- **Status:** Accepted
- **Date:** 2026-06-22
- **Release / context:** The first release (the NBA, with the NBA Cup), and the model for all later releases
- **Related:** [ADR-0001, Record architecture decisions](0001-record-architecture-decisions.md)

## Context

The system must satisfy many architectural characteristics, but the first release targets a small number of readers and low load, on infrastructure operated by one person, within a short timeline. Several important characteristics (scalability, cloud elasticity, application security depth beyond the baseline, regulatory compliance, multi-deployment extraction) are not needed at first-release strength, yet they become important in later releases.

Two observations drive this decision:

1. Discarding future characteristics outright loses the groundwork that makes them cheap to adopt later; treating them all as driving now blows the timeline.
2. Not all characteristics trade off against each other. Some can only be bought at the expense of another (distribution against simplicity). Others are purely additive: they cost extra development and operations effort but impose no structural compromise on the driving set.

## Decision

Classify architectural characteristics **per release** into three bands:

- **Driving:** shape the structure now; the design is optimised for them.
- **Additive:** not driving this release, but we invest *cheap, reversible, seam-creating* groundwork now (abstractions, interfaces, CI and fitness checks, hooks, externalised configuration) so the characteristic can be **promoted later without a structural rewrite**. Additive means effort only, with *no trade-off* against the driving set.
- **Deferred:** no investment this release; revisit when promoted.

**Rules**

1. Characteristics are **release-scoped**: the bands change between releases.
2. Promoting a characteristic to driving **reflects a change in business requirements** (load grew, regulatory exposure grew, a competition must be detached) and is recorded as its own ADR.
3. **Guardrail.** "Additive" can easily become an excuse to build things we don't need yet. So the bar is strict. Additive work is only allowed to be small and easy to undo: an interface where a concrete class would do, a configuration value instead of a hard-coded one, an extra check in the CI pipeline. The moment preparing for a future characteristic starts to require real structure (new components, new infrastructure, more moving parts), it is not additive. In that case we don't build it now: we wait until the need is real, and then decide properly, with its own ADR.

### First-release classification

| Band | Characteristics |
|---|---|
| **Driving** | Reliability; quota-efficiency and cost-governance; testability; modularity |
| **Additive** (cheap groundwork now) | Security and application security depth (the first release's baseline, including the OWASP Top 10, is in scope now and decided in a record of its own; only depth beyond it is additive); traceability; observability depth; scalability and cloud-readiness (statelessness, clean seams, externalised configuration); multi-deployment, and league and competition extraction-readiness; compliance-readiness; adaptability and evolvability |
| **Deferred** (no first-release work) | Elasticity and autoscaling infrastructure; raw performance tuning; microservice decomposition and inter-service communication |

### Additive capacity scales with internal code quality

How much can stay additive, rather than forcing rework, is a function of code quality. A clean, well-factored codebase with clear seams widens the additive band. Traceability, for instance, is additive precisely because the code is written to receive it: correlation-ID propagation points, audit hooks and structured-logging boundaries are already in place, so turning it on is effort, not surgery. Investing in internal quality is therefore what keeps future characteristics cheap to promote, which makes writing it well a first-order architectural concern rather than a stylistic preference.

## Consequences

- The bands describe the current release; band changes are tracked through ADRs.
- Future-version considerations legitimately explain present decisions, for example why a seam exists before it is used.
- **Risk:** "additive" can turn into an excuse for over-engineering. The guardrail (rule 3) and ADR review are the controls.
- This ADR establishes the model only; per-characteristic decisions (for example the security baseline or extraction-readiness) are their own ADRs.
- **Worked example: observability.** Spring Actuator with a Micrometer registry and structured logging are groundwork in exactly the sense the guardrail allows: a dependency, a configuration value, an exposed endpoint. They stay in the first release. A metrics collector and a dashboard product are new infrastructure, so they are not additive: they leave the first release and return with their own ADR when the need is real.
