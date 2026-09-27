# Sports Platform

A spoiler-free sports platform. Its purpose is to let readers access information about sports events without revealing the game result unless they choose to see it. Most sports services deliver game results as quickly as possible, through notifications, headlines, search matches, video thumbnails and social media feeds, and give people no direct control over when an outcome is revealed; people who watch a game later than it was played need the opposite. A game result will be shown only when the reader explicitly asks for it, and the same applies to anything that could indirectly reveal the outcome: partial scores, player statistics, changes in the standings, or images that show how the event ended. The first version will focus on one sport and one league: basketball and the NBA.

> ### Status: no application code
>
> This repository holds documentation, listed under Documentation below, and no application code. The system described here is designed, not built.

## Constraints

Three constraints apply to every part of the system and to every decision made about it. The [system description](docs/system-description.md) states them in full.

- **Data costs nothing; hosting is a small fixed cost.** The system operates within the free allowances that external data providers and the video platform offer, and a paid data source is used only by explicit decision. Those allowances are small and fixed, so how often and how much the system requests from them is limited by the allowances, not by reader demand.
- **Use of media is legally clean.** The system stores a video only if its platform marks it as public and embeddable when it is stored, shows it through that platform's own player, never re-hosts or re-uploads it, and claims no ownership over any provider's data or any creator's video.
- **One person builds and operates the system.** There is no separate operations role and no second person. Every operational choice must be one that a single person can run alongside building the system.

## Documentation

- [System description](docs/system-description.md): the problem the system addresses, what it is meant to accomplish, the constraints that shape it, who and what interacts with it, where its boundary lies, what it excludes, and the scope of the first version
- [Requirements](docs/requirements.md): the foundational behaviour the published specification requires, extended as the work that implements more of it is published
- [Glossary](docs/glossary.md): the meaning each term carries across these documents, and the requirement that owns it where one does

Further documentation is published with the work that needs it and listed here as it lands.

## Contributions and support

**This project is not open to contributions.** It is built and maintained by one person, who is
also its sole operator, and it is intended to stay that way. External pull requests are not
accepted. The repository is public so the work can be read, not so it can be developed
collectively.

## License

Copyright (c) 2026 Angel Zlatenov. All rights reserved.

The code and documentation here are published to be read, not reused. No permission is granted
to copy, modify or redistribute them. This is a deliberate choice rather than an oversight: the
platform is intended to run as a live service maintained by its author.
