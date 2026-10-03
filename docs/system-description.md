# System Description: Phase One

> ### Status
>
> This document describes the system and the problem it addresses. No application behaviour exists yet;
> no part of the described system is built. It is the reference from which the detailed
> requirements and the architecture decisions derive.

## 1. Problem domain

Most sports platforms assume that fans want to know the outcome of a game as soon as it happens. Because of this, scores and game results are shared almost everywhere through notifications, headlines, search matches, video thumbnails, and social media feeds, often without anyone asking for them.

However, some people watch games later instead of live. For them, finding out the outcome beforehand can ruin the experience. They want to choose when they learn the outcome rather than having it revealed automatically.

At the moment, these people usually have two choices. They can avoid sports apps, websites, notifications, and social media until they watch the game, or they can risk having the game spoiled. Mainstream sports services generally do not give people direct control over when game outcomes are revealed.

## 2. Purpose of the system

The purpose of the system is to let readers access information about sports events without revealing the game result unless they choose to see it.

This applies to more than just the final score. The system also needs to hide any information that could indirectly reveal the outcome, such as partial scores, player statistics, changes in the standings, or images that show how the event ended.

The goal is to prevent accidental spoilers completely. A game result should only be shown when the reader explicitly asks for it. Even a single unwanted disclosure could ruin the experience of watching the event later.

## Constraints

Three constraints apply to every part of the system and to every decision made about it.

- **Data costs nothing; hosting is a small fixed cost.** The system operates within the free allowances that external data providers and the video platform offer, so data costs nothing, while hosting is a small fixed monthly cost for two servers. A paid data source is used only by explicit decision. Those allowances are small and fixed, so how often and how much the system requests from them is limited by the allowances, not by reader demand.
- **Use of media is legally clean.** The system stores a video only if its platform marks it as public and embeddable when it is stored, shows it through that platform's own player while checks find that it still plays, never re-hosts or re-uploads it, claims no ownership over any provider's data or any creator's video, gathers information from a source only where that source's terms permit it, and keeps what it saves from a source no longer than those terms allow.
- **One person builds and operates the system.** There is no separate operations role and no second person. Every operational choice must be one that a single person can run alongside building the system.

## 3. Operating context

Readers may access the system at any time and from different time zones. There is no expected schedule for how often they use it. Some may visit regularly, while others may return after several days or weeks to catch up on events they have missed.

A reader may or may not already know which events have taken place. The system therefore allows them to browse events while keeping their outcomes hidden until the reader chooses to reveal them.

Readers can explore information such as participants, schedules, statistics and available highlights without automatically learning the outcome of an event, and they can open a destination that exists to show outcomes, such as the standings, when they choose to. They can reveal individual outcomes when they are ready, while other outcomes remain hidden.

For a Logged-in user, the system remembers which outcomes they have already revealed across sessions, for as long as the account exists; deleting an account deletes everything kept for it. For a Guest nothing is kept: a reveal lives only in the loaded page, and a refresh hides the outcome again.

## 4. Actors

| Actor | Role |
|---|---|
| **Guest** | Browses the available sports content without being logged in. Any game result they reveal lives only in the loaded page; a refresh hides it again and nothing is stored. |
| **Logged-in user** | Uses the same content while logged in. The system remembers which game results they have already revealed, for as long as the account exists, and they can set whether the game results of the current season and of past seasons start hidden or shown. |
| **Administrator** | Handles cases that cannot be resolved automatically, such as loading a season's data, adding missing highlight references, managing trusted video sources, dealing with unavailable recordings, entering an official outcome the league settles by something the system cannot calculate, and handling season transitions. Also runs the system: reads the ingestion failure log, the provider budgets and submitted feedback, and sets how often scheduled work runs. |
| **Scheduler** (internal) | Part of the system, not an outside party: the component that starts work no outside party asks for. It acts on a clock, deciding when the system requests new data from external providers, and it reacts to changes the system has just recorded, such as bringing the search index and the standings up to date once a game result is stored. Requests to providers happen on a schedule or when the Administrator starts provider work, and never because of anything a reader does. It is listed with the actors because it initiates actions that no reader does; its schedule and request limits lie inside the system boundary (section 5). |
| **Data providers** | External services that provide information about events, scores, statistics, standings, and other sports data. The system depends on this data, but providers may have request limits, change their interfaces, or become temporarily unavailable. |
| **Video platform** | Hosts highlight videos created and published by others. The system only stores links or references to these videos and does not host the videos itself. In Phase One, YouTube is used for this purpose. |

## 5. System boundary

**Within the system:**

- stored information about events, participants and their rosters, statistics, player availability and highlight references, and the standings computed from it
- the competition structure prepared for each season
- the logic that decides which information can be shown to a reader without revealing a hidden outcome
- the record of which outcomes a Logged-in user has already chosen to reveal
- the settings and feedback kept for each Logged-in user, deleted with the account
- facts the Administrator enters, such as an official outcome the rules cannot settle, which collected data never overwrites
- saved copies of provider responses, kept private and never redistributed, so that previously collected data can be rebuilt or processed again without making new requests
- the schedule and request limits used when collecting data from external providers
- the ingestion failure log, where everything that needs the Administrator is recorded, from provider requests that failed or returned unusable data to outcomes the rules cannot settle

**Outside the system:**

- the sporting events and competitions themselves; the system only collects information about them.
- the accuracy of the information supplied by external data providers; the system can validate incoming data but does not create the original facts.
- the highlight videos themselves; these remain hosted and owned by their original creators or platforms.
- user identity management, which is handled by an external identity service.
- other websites, apps, social media platforms, or communication channels that may reveal game outcomes outside the system.

## 6. Exclusions

The following areas are intentionally outside the scope of the system.

- **No live event coverage.** The system does not follow an event while it is being played: no live score, no play-by-play, no running commentary. Continuously showing new scores and game results would conflict with the goal of preventing unwanted spoilers. A game that is scheduled or under way still appears, with its schedule and its status and no score; its game result appears once the game has finished and the game result is stored.

- **No notifications to readers.** The first version sends readers no email, push or in-app alert of any kind; everything waits until the reader opens the system. Notifications are one of the channels through which outcomes reach people who want to avoid them, so any future notification is a decision of its own, checked like any other surface.

- **No reader-triggered data requests.** Browsing the system does not cause new requests to external data providers. Readers only see information that has already been collected and stored. This keeps provider usage predictable and independent of the number of readers accessing the system.

- **No hosting of highlight videos.** The system only stores references or links to publicly available highlight videos. The videos themselves remain hosted by the original platform.

- **No guessing missing information.** The system stores only information reported by data providers, the competition structure prepared for each season, facts the Administrator enters and values that can be directly calculated from these. It does not try to guess or invent facts that were not provided: an outcome the league settles by something the system cannot calculate, such as a drawing, stays undecided until the Administrator enters the official one.

## 7. Scope of Phase One

The first version of the system will focus on one sport and one league: basketball and the NBA, with the competitions that league runs in a season (the regular season, the NBA Cup, the play-in and the playoffs).

The system will include the NBA seasons that can be obtained from the available data providers. Earlier seasons are captured while the providers still serve them and become selectable once their competition structure has been prepared, so the first version may launch with the most recent season alone. The current season will be shown by default, while earlier seasons can be selected by the reader.

The interface will support multiple languages. However, sport-specific names and terms, such as team names, player names, and statistic labels, will remain in their original form rather than being translated.
