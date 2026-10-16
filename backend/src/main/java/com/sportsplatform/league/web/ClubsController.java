package com.sportsplatform.league.web;

import com.sportsplatform.league.application.ListCurrentSeasonClubs;
import com.sportsplatform.league.application.SeasonClub;
import com.sportsplatform.league.application.SeasonClubs;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The prepared clubs of the default league's current season, served from the platform's own
 * store. Responds 404 while that league holds no current season.
 */
@RestController
public class ClubsController {

    private final ListCurrentSeasonClubs listCurrentSeasonClubs;

    public ClubsController(ListCurrentSeasonClubs listCurrentSeasonClubs) {
        this.listCurrentSeasonClubs = listCurrentSeasonClubs;
    }

    @GetMapping("/api/clubs")
    public ResponseEntity<ClubsResponse> currentSeasonClubs() {
        return listCurrentSeasonClubs.list()
                .map(ClubsResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record ClubsResponse(String league, String season, List<ClubResponse> clubs) {
        static ClubsResponse from(SeasonClubs seasonClubs) {
            return new ClubsResponse(
                    seasonClubs.season().league().value(),
                    seasonClubs.season().season().value(),
                    seasonClubs.clubs().stream().map(ClubResponse::from).toList());
        }
    }

    public record ClubResponse(String id, String name, String conference, String division) {
        static ClubResponse from(SeasonClub club) {
            return new ClubResponse(
                    club.id().value(), club.name(), club.conference().name(), club.division().name());
        }
    }
}
