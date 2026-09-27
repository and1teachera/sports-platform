# Requirements

> ### Status
>
> This document states the behaviour the published specification currently requires from the
> system. No application code exists yet: every requirement here is decided, not built. It holds
> the foundational requirements that make the [system description](system-description.md)
> precise; further requirements are published with the work that implements them, under the same
> headings.

## Reading this document

Each requirement has a title and one statement. Other documents cite a requirement by its title. Requirements are grouped by area, and the grouping is kept clear as the set grows rather than fixed by publication order; a reference affected by a change is updated in the same change.

The actors are those of the [system description](system-description.md): a **reader** is a Guest or a Logged-in user of the public application, and the Administrator operates the system. The [glossary](glossary.md) gives the meaning of every term these requirements use, among them game result, score, outcome-bearing information and reveal.

## Game listings

### Reveal Game Results

Readers reveal a game result by an explicit action on the game's widget: pressing its control that shows the game result, opening its statistics or its compare view, or pressing play on one of its highlight tiles. Two other controls reveal games: a season high's control on a player's page reveals the game the high came from, and the control on a masked knockout game's widget reveals the games that decided it. The rule for each of those two controls is published with the work that implements it. Nothing else reveals a game result, and seeing a game listed, wherever it is listed, reveals nothing. Destinations that exist to show outcomes, such as the standings, the standings up to a chosen date, the Cup group tables, a pre-game subsection and a player's season line apart from which games its season highs came from, disclose them by design, because opening one is itself an explicit ask.

### Hidden Everywhere by Default

Outcome-bearing information, as [What Outcome-Bearing Means](#what-outcome-bearing-means) defines it, is hidden by default on every surface: listings, game widgets, statistics, standings views, search matches, highlights, brackets. Not only the homepage. Some destinations are themselves the reader's explicit ask, and show outcome-bearing information in full by design: opening the standings or choosing a date on them, opening the Cup section, whose group tables show filled in while each knockout tie shows its game as a game widget, opening a pre-game subsection, and opening a player's season line, apart from which games its season highs came from.

### No Score While A Game Is Under Way

The system never displays a score for a game that is under way. A revealed game that is under way shows its status only; its game result appears once it is stored.

### Reveal Persistence (Authenticated)

For logged-in users, an explicit reveal is remembered per user and game; revealed games stay revealed across refreshes and later visits.

### Reveal Scope (Anonymous)

For readers who are not logged in, a reveal lasts only as long as the loaded page. It survives in-app navigation, but a refresh or a new visit hides the game again. Nothing about a guest's reveals is stored, on the client or on the server. Signing in does not adopt the reveals a guest made before it, so afterwards the reader sees what the account itself remembers.

### Game Status Is Visible

A game is in exactly one of these states, and the state is always visible to a reader: scheduled, under way, finished, postponed, cancelled. None of them says who won, and a reader needs the state in order to decide whether there is anything to watch. A finished game must be distinguishable from a cancelled one. One exception exists: what a reader sees of a playoff series game that may not be needed follows a rule published with the playoffs work.

### Calendar Date Selection

A calendar lets a reader select a date and see that date's games, for any date in the season. The calendar marks the dates that have games.

### Competition And Phase Are Named

Each game names both the competition and the phase it belongs to, read from the season's prepared structure. The two are different levels and a game always names both. A season holds competitions and a competition holds one or more named phases: the regular season is a competition with a single phase, the Cup is a competition whose phases are its group stage and its knockout stage, and the playoffs are a competition whose phases are its rounds. One exception: a fixture added during the season that the Administrator has not placed yet names neither, until it is placed in the Cup or marked as an ordinary game of the regular season.

### Seasons Stay Available

A season the system has already collected remains available to readers afterwards, including after the data provider stops serving that season. The system does not depend on being able to fetch a season again.

### What Outcome-Bearing Means

Outcome-bearing information is any value from which a reader could work out who won a game they have not asked about. It is not the score alone. It includes the score, a player's statistical line for that game, a team's win-loss record, a team's position or games-behind in a table, a winning or losing streak, a last-ten record, and which team advanced in a knockout bracket. A surface is checked by asking whether an outcome can be worked out from it, never by checking whether it carries a field named score.

### Reveal Records Are Kept Indefinitely

A logged-in user's record of which games they have revealed is never removed. There is no expiry and no clearing. A logged-in user who returns after two years finds the games they had already revealed still showing their game results, which is the behaviour that makes the product work on a return visit. Never removed means never expired or cleared while the account exists: when the account is deleted, its reveal record is deleted with it.

## Resilience

### Serving Is Local

Every reader-facing read is served from the local store, so a provider outage has no effect on what readers see; it delays only how fresh the stored data is. This is the normal path, not a fallback.

## Authorization

### Role-Based Access

Two roles: USER and ADMIN. Endpoints enforce role-based access control.

## Security

### Viewing-Data Privacy

Per-user reveal records and preferences are personal data; they never appear in shared caches, shared payloads, or public endpoints. Feedback and broken-link reports, which always come from an account, are personal data as well.

## Internationalization

### Supported Languages

The system supports 5 languages: English, Spanish, German, French, Bulgarian. The public application is translated into all of them. Administrative content is English only and is not translated.

### Domain Content Exclusion

Domain content (team names, player names, stat labels) is not translated. The rule binds every surface a reader sees.

## Responsive design

### Multi-Device Support

The system provides a responsive layout optimized for desktop, tablet, and mobile devices.
