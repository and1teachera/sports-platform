# System Description: Phase One

> ### Status
>
> This document describes the system and the problem it addresses. It precedes implementation;
> no part of the described system exists. The first release is targeted for October 2026.
>
> It names no technologies and enumerates no features. Detailed behavioural requirements are
> held in the requirements list, and the rationale for technical choices in the architecture
> decision records. This document is the reference from which both derive.

## 1. Problem domain

Most sports platforms assume that fans want to know the result of a game as soon as it happens. Because of this, scores and results are shared almost everywhere through notifications, headlines, search results, video thumbnails, and social media feeds, often without the user asking for them.

However, some people watch games later instead of live. For these users, finding out the result beforehand can ruin the experience. They want to choose when they learn the outcome rather than having it revealed automatically.

This creates a conflict between two different ways of following sports. Most sports services are designed to deliver results as quickly as possible, while users who watch games later need the exact opposite: they need those results to stay hidden until they are ready to see them.

At the moment, these users usually have two choices. They can avoid sports apps, websites, notifications, and social media until they watch the game, or they can risk having the result spoiled. Mainstream sports services generally do not give users direct control over when game outcomes are revealed.

## 2. Purpose of the system

The purpose of the system is to let users access information about completed sports events without revealing the result unless they choose to see it.

This applies to more than just the final score. The system also needs to hide any information that could indirectly reveal the outcome, such as partial scores, player statistics, changes in the standings, or images that show how the event ended.

The goal is to prevent accidental spoilers completely. A result should only be shown when the user explicitly asks for it. Even a single unwanted disclosure could ruin the experience of watching the event later, so avoiding spoilers is a core requirement of the system.

## 3. Operating context

Users may access the system at any time and from different time zones. There is no expected schedule for how often they use it. Some may visit regularly, while others may return after several days or weeks to catch up on events they have missed.

A user may or may not already know which events have taken place. The system therefore allows them to browse completed events while keeping their outcomes hidden until the user chooses to reveal them.

Users can explore information such as participants, statistics, standings, and available highlights without automatically learning the result of an event. They can reveal individual outcomes when they are ready, while other results remain hidden.

For logged-in users, the system remembers which outcomes they have already revealed across sessions. For users who are not logged in, this information is only kept for the current session.

The system only controls what is shown within its own interface. It cannot prevent spoilers from appearing through external sources such as social media, news websites, messaging apps, or other sports services.
## 4. Actors

| Actor                 | Role                                                                                                                                                                                                                                               |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Unidentified user** | Browses the available sports content without being logged in. Any results they reveal are remembered only for the current session.                                                                                                                 |
| **Identified user**   | Uses the same content while logged in. The system remembers which results they have already revealed across sessions, and they can configure how result disclosure should behave by default. More features will follow for this actor.             |
| **Administrator**     | Handles cases that cannot be resolved automatically, such as adding missing highlight references, managing trusted video sources, dealing with unavailable recordings, and handling season transitions.                                            |
| **Scheduler**         | Controls when the system requests new data from external providers. These requests happen according to a schedule and are not triggered by user actions.                                                                                           |
| **Data providers**    | External services that provide information about events, scores, statistics, standings, and other sports data. The system depends on this data, but providers may have request limits, change their interfaces, or become temporarily unavailable. |
| **Video platform**    | Hosts highlight videos created and published by others. The system only stores links or references to these videos and does not host the videos itself. In phase one, YouTube is used for this purpose.                                            |

## 5. System boundary

**Within the system:**

- stored information about events, participants, statistics, standings, and highlight references
- the logic that decides which information can be shown to a user without revealing a hidden result
- the record of which outcomes an identified user has already chosen to reveal
- user settings related to how results are displayed
- saved copies of provider responses so that previously collected data can be rebuilt or processed again without making new requests
- the schedule and request limits used when collecting data from external providers
- records of provider requests that failed or returned unusable data

**Outside the system:**

- the sporting events and competitions themselves; the system only collects information about them.
- the accuracy of the information supplied by external data providers; the system can validate incoming data but does not create the original facts.
- the highlight videos themselves; these remain hosted and owned by their original creators or platforms.
- user identity management, which is handled by an external authentication service.
- other websites, apps, social media platforms, or communication channels that may reveal sports results outside the system.

## 6. Exclusions

The following areas are intentionally outside the scope of the system.

- **No live event coverage.** The system focuses on completed events. Covering events while they are happening would require continuously showing new scores and results, which conflicts with the goal of preventing unwanted spoilers.
    
- **No user-triggered data requests.** Browsing the system does not cause new requests to external data providers. Users only see information that has already been collected and stored. This keeps provider usage predictable and independent of the number of users accessing the system.
    
- **No hosting of highlight videos.** The system only stores references or links to publicly available highlight videos. The videos themselves remain hosted by the original platform.
    
- **No guessing missing information.** The system only stores information reported by trusted providers and values that can be directly calculated from that data. It does not try to guess or invent facts that were not provided.
    

## 7. Scope of phase one

The first version of the system will focus on one sport and one competition: basketball and the NBA.

The system will include the NBA seasons that can be obtained from the available data providers. The most recent season will be shown by default, while previous seasons can be selected by the user.

The interface will support multiple languages. However, sport-specific names and terms, such as team names, player names, and statistic labels, will remain in their original form rather than being translated.
