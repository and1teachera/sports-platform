# Phase One scope

> ### Status
>
> This document says what the first version of the system, Phase One, is intended to deliver and
> what it deliberately leaves out. No application code exists yet; everything here is designed,
> not built. The [system description](system-description.md) states the problem, the
> purpose, the constraints, the actors and the boundary. The [requirements](requirements.md) state
> the behaviour the published specification requires, and gain detail as the work that implements
> it is published.

## The guarantee

The [system description](system-description.md) states the purpose: to let readers access information about sports events without revealing the game result unless they choose to see it. Stated so that it can be checked rather than asserted, the goal has two parts. No response the system sends to a reader carries outcome-bearing information unless that reader explicitly asked for it, either by naming the event, by opening a destination that exists to show outcomes, or by a visibility setting the reader chose that shows that event's season. Outcome-bearing means any value from which a reader could work out who won, not the score alone; the requirement [What Outcome-Bearing Means](requirements.md#what-outcome-bearing-means) defines it in full. And every surface that can carry outcome-bearing information is tested twice, once for the case where the reader has not asked, and once for the case where something the surface depends on fails. The second part matters because a promise of this kind is judged by what happens when it breaks, not by what happens when everything works.

The requirements [Hidden Everywhere by Default](requirements.md#hidden-everywhere-by-default) and [Reveal Game Results](requirements.md#reveal-game-results) state the rule in its normative form: what is hidden, what counts as asking, and which destinations disclose outcomes by design.

## One sport, one league

The first version covers basketball and the NBA, with the competitions the league runs in a season: the regular season, the NBA Cup, the play-in and the playoffs. The preseason is not part of the first version. A season's competitions, the teams taking part with their conference and division, and the Cup group assignments are prepared data rather than code; the first version ships them for the 2026-2027 season and has no screen for editing them, so a later season is another set of prepared data rather than a code change.

The current season is shown by default and earlier seasons can be selected. Earlier seasons are captured while the data providers still serve them and become selectable once their competition structure has been prepared, so the first version may launch with the most recent season alone. A season the system has collected stays available afterwards, as [Seasons Stay Available](requirements.md#seasons-stay-available) requires.

## What readers will be able to do

- **Game listings.** The homepage shows the games of the most recent playing day without revealing game results; readers move to other days and pick dates in a calendar that marks the dates with games. Every game shows its competition, its phase and its status, as [Competition And Phase Are Named](requirements.md#competition-and-phase-are-named) and [Game Status Is Visible](requirements.md#game-status-is-visible) require. Game results are hidden by default and revealed one game at a time by an explicit action on the game, as [Hidden Everywhere by Default](requirements.md#hidden-everywhere-by-default) requires; a Guest's reveal lasts for the loaded page, as [Reveal Scope (Anonymous)](requirements.md#reveal-scope-anonymous) states, and a Logged-in user's is kept, as [Reveal Persistence (Authenticated)](requirements.md#reveal-persistence-authenticated) states.
- **Player statistics.** A game's basic player statistics open on the game, two players from the same game can be compared side by side, and a player's page shows a season line computed on request from the player's stored games, with a season high in each category.
- **Video highlights.** For each game, short and extended highlights from the video platform, embedded in that platform's own player. Several clips of each type are stored, so that the next one takes over when one stops playing, and when none remains the highlights say so plainly. A Logged-in user can report a clip that has stopped playing.
- **Standings.** The regular-season table, under the filters a reader chooses, as it stands now or as of any date, with ties broken by the league's official rules and never by house rules. The NBA Cup's group tables and knockout bracket, and a playoffs view with the play-in and each round's series.
- **Search.** Players, teams and games, by name and by filters, tolerant of misspellings.
- **Schedule and team pages.** The full schedule, filtered by date, team or competition, and a page for every team with its games, its roster and the injury and suspension entries for its players.
- **Injury and suspension reports, and pre-game analysis.** A league-wide injury and suspension report, browsable freely because it carries no outcome. On a game, a pre-game analysis that is hidden by default and opened one subsection at a time, every value as it stood before that game.
- **User preferences.** Anyone can set the display preferences; a Logged-in user's choices are kept. A Logged-in user can also choose a favourite team, whose games are listed first, and set whether the game results of the current season and of past seasons start hidden or shown.
- **Feedback.** A Logged-in user can send feedback.
- **Languages and devices.** The public application in five languages, as [Supported Languages](requirements.md#supported-languages) lists, with team names, player names and statistic labels untranslated, as [Domain Content Exclusion](requirements.md#domain-content-exclusion) requires, and a responsive layout across devices, as [Multi-Device Support](requirements.md#multi-device-support) requires.

## What the Administrator will do

The Administrator loads a season's teams, rosters and schedule from the providers, moves the league to a new season, places each Cup game in its group or knockout tie, enters the official outcomes the rules cannot settle from stored data, and corrects which tables a game counts toward when the official ruling differs from the computed one. The Administrator maintains the highlights where the automated process falls short: supplying, removing and reordering clips, managing the trusted channels that rank candidates, resolving the broken-link reports Logged-in users send, and working through the highlight issues the system raises. And the Administrator runs the system: the ingestion failure log, where everything that needs a person is recorded; the provider budgets and their consumption; how often scheduled work runs; the feedback Logged-in users sent; the repair of derived data; a player's availability set by hand; and the deletion of a Logged-in user's account on request.

## How data will arrive

Stored information has three origins: what the data providers and the video platform report, the facts the Administrator enters, such as an official outcome the rules cannot settle from stored data, and the values computed from what is already stored, such as the standings. Collection from the providers and the video platform happens on a schedule and within the free allowances those services offer, and a reader's browsing never causes a request to a provider. Every reader-facing read is served from what the system has stored, as [Serving Is Local](requirements.md#serving-is-local) requires. Highlights are looked for after a game result is stored, within a limited window and budget, so they follow the game rather than accompany it, and a game may end up with none. Standings are computed from stored game results and never fetched for display. Saved copies of provider responses are kept privately so that stored data can be rebuilt without new requests.

## Outside Phase One

The [system description](system-description.md) states the exclusions in full: no live event coverage, no notifications to readers, no reader-triggered data requests, no hosting of highlight videos, and no guessing of missing information. In addition, the first version has:

- no sport or league other than basketball and the NBA, and no preseason games;
- no native mobile applications; the public application is a responsive web application;
- no screen for editing a season's competition structure, which arrives as prepared data;
- no paid data source, unless one is adopted by explicit decision.

The system controls only what it shows. Other websites, apps, social media platforms and communication channels that reveal game outcomes are outside it, as the system description's boundary states.
