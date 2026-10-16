package com.sportsplatform.league.application;

import com.sportsplatform.league.domain.SeasonStructureId;

import java.util.List;

/**
 * Read port over the platform's own store: the clubs placed in a season, one entry per season
 * placement, ordered by club id.
 */
public interface SeasonClubQuery {
    List<SeasonClub> placedIn(SeasonStructureId season);
}
