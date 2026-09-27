# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-06-22
- **Release / context:** Whole project

## Context

The architecture will change across multiple releases by design: scope grows (more competitions, then more leagues), infrastructure changes (a hosted virtual private server first, scalable cloud services later), and the priority of architectural characteristics is re-evaluated each release. The reasoning must be preserved, not only the decision. A later change needs to know why the current shape exists, or it will either repeat an option already rejected or reverse a decision without knowing what that decision buys.

## Decision

Use **Architecture Decision Records** for every significant decision. ADRs are numbered sequentially and move through the statuses Proposed, Accepted and, later, Superseded by a later record or Deprecated. ADRs are treated as a **primary, first-class deliverable**, not an afterthought. Re-prioritisation of characteristics between releases is itself recorded as an ADR.

ADRs build on each other, and a later record may refine or supersede an earlier one. From the start of development that is the only way a decision changes: a new ADR replaces the old one, which is marked superseded.

## Consequences

- Every structural decision carries a traceable record of context and consequences.
- Why the architecture changed between releases stays recoverable, so a decision can be revisited on its own terms instead of re-argued from scratch.
- Small ongoing documentation cost in exchange for large long-term traceability benefit.
