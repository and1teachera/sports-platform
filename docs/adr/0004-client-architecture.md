# ADR-0004: Two client applications, chosen against their own constraints

- **Status:** Accepted
- **Date:** 2026-08-30
- **Release / context:** The first release's client architecture; mobile deferred
- **Related:** [ADR-0003, Modular monolith architecture](0003-modular-monolith-architecture.md) (the API these clients consume); the [system description](../system-description.md); the [requirements](../requirements.md)

## Context

The system has two interactive surfaces, and their constraints differ substantially.

The **public surface** is reachable without authentication, and anonymous use is its default rather than an edge case. It is served in five languages. It is read-mostly, and its pages must render and be cacheable with no session attached to them. A large part of its content changes rarely, such as the pages of past seasons: a past season's tables change only when a correction recomputes them, and every table that depends on the correction is recomputed, so those pages can be produced once, served from cache, and refreshed when a correction recomputes what they show. Its traffic is public and unbounded.

The **administrative surface** is reachable only by the Administrator, who holds the ADMIN role, and there is one such person. It is English only, which the [requirements](../requirements.md#supported-languages) state explicitly. It is never indexed and never publicly cached. Its work is form and table shaped, such as maintaining trusted video sources, entering highlight references by hand, triaging reported dead links, placing Cup games and performing season transitions. First-load cost is immaterial behind a login used by one person.

Choosing one client technology for both means choosing it against the wrong constraints for one of them.

A further constraint arrives from the roadmap: mobile clients are planned for a later release. Whatever serves the public surface in the first release must not become something a mobile client cannot reuse.

## Decision

**1. Two client applications in the first release, each chosen against its own surface.**

The public application uses **Next.js with React and TypeScript**. The deciding properties are server-side rendering without a session, public cacheability, and static generation for content that changes rarely. A past season's pages are generated once and served from cache until a correction recomputes what they show, when every generated page and cached copy that shows it is refreshed. That applies quota-efficiency and reliability under constraint to the read path rather than only to ingestion.

The administrative application uses **Angular with TypeScript**. The deciding property is that the surface is almost entirely forms and tables, which reactive forms, validation and the CDK table primitives address directly. The costs that would count against Angular on the public surface, meaning payload size, server-rendering setup and five-language translation, do not apply to a single-administrator, single-language surface behind a login. Component libraries and styling tooling are implementation choices, made when the clients are built.

**2. Both are clients of the same API, and neither is a second backend.** Neither application reaches the database. The public application may render on the server, but it renders from API responses; it holds no business logic and no data access of its own. Server route handlers do not become a backend-for-frontend that only the public client can call. This rule is what keeps a third client possible without a rewrite.

**3. Spoiler safety is not implemented in any client.** It stays in the read model, where a decision of its own puts it. A client receives outcome-bearing values only for what its reader has asked for: by naming the event, as a reveal does, by a visibility setting the reader chose, or by opening a destination that is itself an explicit ask, such as the standings, whose shared, cacheable response carries those values identically for every reader. No client-side filtering, no hiding in the view layer. The reveal actions and the destinations that count as an explicit ask are those [Reveal Game Results](../requirements.md#reveal-game-results) lists.

**4. The public application runs as its own container.** Server rendering needs a Node runtime, which Nginx cannot provide, so the deployment carries a container for it, with Nginx serving the administrative application's static build, terminating TLS, and reverse proxying both the API and the public application. The cost is paid deliberately: without the Node runtime the public application would be a client-rendered bundle served statically, which is what the administrative stack already provides, and rule 1 would be left without a reason.

**5. Mobile is deferred** and returns with its own ADR when the public application exists.

## Consequences

- Two build pipelines, two dependency trees and two test setups, maintained by one person. This is a real recurring cost and it is accepted deliberately rather than overlooked.
- The two surfaces share no design system and do not need to. They have different audiences, different constraints, and no requirement that the administrative panel resemble the product. The usual objection to two client stacks, duplicated visual language, does not apply here.
- The API contract has two consumers, so a change to it lands in two places. Generating client types from a single API description is the mitigation, and it should be settled before the second application is started rather than after.
- The clients are consumers and take no part in the modular-monolith decomposition, which is a property of the backend domain ([ADR-0003, Modular monolith architecture](0003-modular-monolith-architecture.md)) and does not depend on how many applications call the API. What two clients do pressure is the **API contract**, because neither can quietly work around a gap the way a single client would.
- Two independently built clients are a genuine test of the read model's serving rules. If spoiler safety is really a property of the read model, a second client written later inherits it without doing anything. If the first client is silently compensating, the second one exposes that. One client alone cannot show the difference.
- Server-side rendering of the public surface is safe **only because** of rule 3. The server renders a page from shared responses, and a shared response carries outcome-bearing values only for a destination that is itself an explicit ask, where every reader who opens it has asked for exactly those values. What a reader has revealed, and a visibility setting that shows a season, arrive in user-scoped or explicitly requested responses and are composed in the client. So pages can be cached and shared publicly without disclosing anything a reader did not ask for. A system that filtered game results in view code could not render on the server at all without either leaking or rendering nothing. The read-model decision is what makes the public client's chosen strategy available.
- Deferring mobile preserves the option only while rule 2 holds. If the public application acquires its own data access, adding a mobile client becomes a rewrite rather than an addition.
