# Glossary

> ### Status
>
> This document gives the meaning each term carries across the published documentation. It fixes
> vocabulary rather than stating behaviour: where a requirement owns a term's normative
> definition, the entry gives the short meaning and links that requirement. Where a rule is not
> yet published, the entry gives the meaning only, and the rule arrives with the work that
> implements it. It holds the terms the published documents rely on; a term is added with the
> document that needs it. Entries are grouped by area and alphabetical within a group.

## People and parties

- **Administrator.** The one person who operates the system and handles what it cannot settle automatically, working in the administrative application behind a login. The [system description](system-description.md) lists what the role covers.
- **Data provider.** An external service the system requests sports data from.
- **Guest.** A reader who is not logged in. Nothing about them is stored, and a reveal they make lasts only as long as the loaded page, as [Reveal Scope (Anonymous)](requirements.md#reveal-scope-anonymous) states.
- **Identity service.** The external service that performs registration, login, password reset and session management, Keycloak in Phase One. User identity management lies outside the system boundary.
- **Logged-in user.** A reader who is logged in. What the system keeps for them lasts as long as the account exists: the games they have revealed, as [Reveal Persistence (Authenticated)](requirements.md#reveal-persistence-authenticated) states, and the settings they choose.
- **Reader.** The general word for a person reading the public application, covering both a Guest and a Logged-in user. A statement that holds for either uses it.
- **Scheduler.** The part of the system that starts the work no outside party asks for: it acts on a clock, and it reacts to changes the system has just recorded.
- **Video platform.** The external platform that hosts the highlight videos, YouTube in Phase One. The system stores a reference to a clip and never hosts a video itself.

## Game results, scores and reveals

- **Compare view.** The side-by-side comparison of two players from the same game, opened on the game's widget. Opening it is one of the actions that reveal the game, as [Reveal Game Results](requirements.md#reveal-game-results) lists.
- **Game result.** The final outcome of a game once it is stored: the winner, the final score and the score of each quarter and overtime period, and whatever derives from them and would disclose them. It is hidden by default on every surface, as [Hidden Everywhere by Default](requirements.md#hidden-everywhere-by-default) requires.
- **Game status.** A game's position in its lifecycle, one of exactly five values and no others: scheduled, under way, finished, postponed, cancelled. None of them says who won, and the status is always visible, as [Game Status Is Visible](requirements.md#game-status-is-visible) requires.
- **Masked.** The state of a team, a title or a thumbnail that a surface withholds because naming it would disclose an outcome.
- **Outcome-bearing information.** Any value from which a reader could work out who won a game they have not asked about, which is a wider category than the score alone. [What Outcome-Bearing Means](requirements.md#what-outcome-bearing-means) defines it in full.
- **Result.** A game's outcome is a game result; what a search returns is a search match.
- **Reveal.** The explicit action on a game that discloses its game result; [Reveal Game Results](requirements.md#reveal-game-results) lists the actions that count and states that nothing else reveals a game. For a Logged-in user a reveal is also a stored record, kept while the account exists, as [Reveal Records Are Kept Indefinitely](requirements.md#reveal-records-are-kept-indefinitely) requires, while for a Guest it lasts only as long as the loaded page.
- **Score.** The numeric points of a team in a game or in one of its periods, and nothing else. A score is never displayed for a game that is under way, as [No Score While A Game Is Under Way](requirements.md#no-score-while-a-game-is-under-way) requires.
- **Season line and season high.** A player's totals and averages for a season. The line holds a season high in each category, the highest single-game value.
- **Spoiler-free.** The covenant the system keeps: nothing that would disclose an outcome reaches a reader who did not choose to see it. The [Phase One scope](phase-one-scope.md) states the guarantee in the form in which it can be checked.
- **Statistics.** A player's line for one game, opened on the game's widget; opening it is one of the actions that reveal the game, as [Reveal Game Results](requirements.md#reveal-game-results) lists. On a player's page, the player's season line.

## Surfaces

- **Bracket.** The knockout bracket of the Cup, the play-in or the playoffs: its shape, its ties and its schedule. Which team advanced in it is outcome-bearing information, as [What Outcome-Bearing Means](requirements.md#what-outcome-bearing-means) states.
- **Calendar.** The per-month view from which a reader picks a date and sees that date's games, as [Calendar Date Selection](requirements.md#calendar-date-selection) requires.
- **Game widget.** The self-contained piece of a page that shows one game wherever the public application lists games, with its game result, its statistics, its compare view, its highlights and its pre-game analysis each behind its own control.
- **Highlight tile.** The card on a game's widget standing for one highlight clip. Pressing play on it is one of the actions that reveal the game, as [Reveal Game Results](requirements.md#reveal-game-results) lists.
- **Listing.** A list of games for a date or a filter, each shown as a game widget. Seeing a game listed is not a reveal, as [Reveal Game Results](requirements.md#reveal-game-results) states.
- **Pre-game analysis.** Context from before a game, reached through a control on the game's widget and opened one subsection at a time. Opening a subsection is itself an explicit ask, as [Reveal Game Results](requirements.md#reveal-game-results) states.
- **Search match.** What a search returns: a player, a team or a game matched by the query.
- **Standings.** A table ranking a season's participants; the bare word means the regular-season table, as it stands now or as of a chosen date, and the Cup's group tables are named explicitly. A standings view exists to show outcomes, so opening one is itself an explicit ask, as [Reveal Game Results](requirements.md#reveal-game-results) states.
- **Team page.** A team's page: its games, its roster and the injury and suspension entries for its players.

## Competitions and seasons

- **Competition.** A distinct contest a league runs within a season. In Phase One these are the regular season, the NBA Cup, the play-in and the playoffs.
- **Competition structure.** The data a season is set up from: its competitions and their phases, the teams taking part with their conference and division, and the Cup group assignments. It is prepared data rather than code, as the [Phase One scope](phase-one-scope.md) states.
- **Conference and division.** The groupings the league divides its teams into for a season; a team's conference and division membership is season-scoped.
- **Current season.** The season the word current points at, shown by default, as against an earlier season a reader selects.
- **Fixture.** A game as the schedule gives it, with its two teams, its date and its start time; it is the shape a game arrives in. A fixture the schedule adds during the season names no competition and no phase until the Administrator has placed it, as [Competition And Phase Are Named](requirements.md#competition-and-phase-are-named) states.
- **Game.** A scheduled contest between two teams, home and away, on a date, within a competition and a phase of a season, as [Competition And Phase Are Named](requirements.md#competition-and-phase-are-named) requires; the unit a game result attaches to.
- **League.** An organising body within a sport: the NBA. The first version covers one league.
- **NBA Cup.** A competition whose phases are its group stage and its knockout stage, as [Competition And Phase Are Named](requirements.md#competition-and-phase-are-named) states.
- **Phase.** A segment of a competition whose games all follow one format, such as the Cup's group stage, its knockout stage or a single playoff round. A season holds competitions and a competition holds one or more named phases, and a game names both, as [Competition And Phase Are Named](requirements.md#competition-and-phase-are-named) requires.
- **Phase One.** The name of the first release of the system, whose contents the [Phase One scope](phase-one-scope.md) states. It names a release and not a phase of a competition.
- **Play-in and playoffs.** Two competitions whose phases are their rounds; [Competition And Phase Are Named](requirements.md#competition-and-phase-are-named) states it for the playoffs, and a playoff round is played as a best-of series.
- **Schedule.** The set of a season's games with their dates, teams, venues and times.
- **Season.** The yearly edition of a league's competitions, such as the 2026-2027 season, and the axis along which the set of competitions and who takes part in them vary. A season the system has collected stays available afterwards, as [Seasons Stay Available](requirements.md#seasons-stay-available) requires.

## Teams and players

- **Player.** A league-level identity that persists across seasons; a player takes part in a season through a team's roster.
- **Player availability.** A player's entry in the league-wide injury and suspension report. The word injury names the record even when the status is a suspension.
- **Roster.** The set of players taking part for a team in a season.
- **Team.** A word with three uses, and each document says which it means: a participant in a game, the league-level identity that persists across seasons (a rename is a rename, a relocation a succession), and a row in a standings table. Where the league-level identity is meant and clarity helps, club or organization is written instead.

## Accounts and settings

- **Account.** What a Logged-in user has and a Guest does not. What the system keeps for a reader is held against the account and deleted with it, as [Reveal Records Are Kept Indefinitely](requirements.md#reveal-records-are-kept-indefinitely) states of the reveal record.
- **Default game result visibility.** Two settings a Logged-in user holds, one for the current season and one for past seasons: whether that season's game results start hidden or shown; a Guest has neither. The [system description](system-description.md) names the setting in its actor table.
- **Display preferences.** The display theme, the timezone, the language, the time format and the date format. A Guest can change them for the visit; a Logged-in user's choices are kept.
- **Favourite team.** A team a Logged-in user chooses; a Guest has none.
- **Feedback.** What a Logged-in user sends about the platform; a Guest cannot send it.
- **Personal data.** A reader's reveal records and preferences, and the feedback and broken-link reports they sent. It never appears in shared caches, shared payloads or public endpoints, as [Viewing-Data Privacy](requirements.md#viewing-data-privacy) requires.
- **Roles.** USER and ADMIN, the two roles endpoints enforce, as [Role-Based Access](requirements.md#role-based-access) requires. A Logged-in user holds USER; the Administrator holds ADMIN.

## Video

- **Broken-link report.** A Logged-in user's report that a clip no longer plays.
- **Highlight.** A video clip of a game, and nothing else.
- **Highlight issue.** The Administrator's record that a game's highlight type has no working clip.
- **Re-check.** The periodic check that a stored clip still plays.
- **Short and extended highlight.** The two highlight types per game: a short clip of a game, and its full highlights.
- **Trusted channel.** A video platform channel the Administrator lists as trusted.

## Data collection

- **Ingestion failure log.** The one list of records written for the Administrator: everything that needs a person, from provider requests that failed or returned unusable data to outcomes the rules cannot settle.
- **Local store.** What the system has stored. Every reader-facing read is served from it, as [Serving Is Local](requirements.md#serving-is-local) requires.
- **Provider allowance.** The free request allowance a data provider or the video platform offers, small and fixed.
- **Saved provider responses.** Copies of provider responses kept as received, privately and never redistributed, so that stored data can be rebuilt without new requests.

## Architecture

- **Administrative application.** The client application for the Administrator, behind a login and English only, written in Angular with TypeScript. It is a client of the same API as the public application, as [ADR-0004](adr/0004-client-architecture.md) decides.
- **Bounded context.** A module that owns its own model, its own vocabulary and its own data, and reaches another module only across a defined boundary. Which bounded contexts the application will hold is not yet decided.
- **Container.** One of the runtime units the system deploys under Docker Compose. The [architecture overview](architecture.md) names them and shows how they relate.
- **Hexagonal (ports and adapters).** The layering used inside each bounded context: a domain core that depends on nothing outside itself, the ports through which it is driven and through which it asks for what it needs, and the adapters at the edges that implement those ports.
- **Modular monolith.** One deployable application whose bounded contexts are modules with enforced boundaries rather than separately deployed services, as [ADR-0003](adr/0003-modular-monolith-architecture.md) decides.
- **Public application.** The client application for readers, reachable without signing in, written in Next.js with React and TypeScript. It renders pages on the server from API responses and holds no business logic and no data access of its own, as [ADR-0004](adr/0004-client-architecture.md) decides.
- **Quota governor.** The component that holds the request budget for each data provider.
- **Read model.** The side of the backend that composes what a surface is served, and where spoiler safety lives rather than in any client. A response shared between readers carries outcome-bearing values only for a destination that is itself an explicit ask, while what an individual reader has asked for arrives in a response scoped to them, as [ADR-0004](adr/0004-client-architecture.md) states.
