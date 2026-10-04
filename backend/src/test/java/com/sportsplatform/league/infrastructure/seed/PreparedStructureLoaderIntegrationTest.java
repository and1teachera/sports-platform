package com.sportsplatform.league.infrastructure.seed;

import com.sportsplatform.league.application.ApplyOutcome;
import com.sportsplatform.league.application.ApplyPreparedStructure;
import com.sportsplatform.league.domain.ClubId;
import com.sportsplatform.league.domain.ClubRepository;
import com.sportsplatform.league.domain.SeasonStructureId;
import com.sportsplatform.league.domain.SeasonStructureRepository;
import com.sportsplatform.support.DatabaseCleanedIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seed loader parses a tiny fixture, maps it to a prepared structure, and hands it to the
 * apply operation. The three outcomes are exercised end-to-end against PostgreSQL.
 */
@SpringBootTest
class PreparedStructureLoaderIntegrationTest extends DatabaseCleanedIntegrationTest {

    @Autowired private PreparedStructureLoader loader;
    @Autowired private ApplyPreparedStructure apply;
    @Autowired private ClubRepository clubs;
    @Autowired private SeasonStructureRepository structures;
    @Autowired private JdbcClient jdbc;

    @DisplayName("The fixture applies on an empty database")
    @Test
    void shouldApplyFixtureOnEmptyDatabase() {
        var prepared = loader.load(new ClassPathResource("seed/test-fixture.yaml"));

        var outcome = apply.apply(prepared);

        assertThat(outcome).isInstanceOf(ApplyOutcome.Applied.class);
        assertThat(clubs.find(new ClubId("club-001"))).hasValueSatisfying(c ->
                assertThat(c.name()).isEqualTo("Boston Celtics"));
        assertThat(clubs.find(new ClubId("club-021"))).hasValueSatisfying(c ->
                assertThat(c.name()).isEqualTo("Golden State Warriors"));
        assertThat(structures.find(prepared.structure().id())).isPresent();
        assertThat(countRefusals()).isZero();
    }

    @DisplayName("Applying the same fixture twice leaves the second apply Unchanged")
    @Test
    void shouldLeaveSecondApplyUnchanged() {
        var prepared = loader.load(new ClassPathResource("seed/test-fixture.yaml"));
        apply.apply(prepared);

        var second = apply.apply(prepared);

        assertThat(second).isInstanceOf(ApplyOutcome.Unchanged.class);
        assertThat(countRefusals()).isZero();
    }

    @DisplayName("A fixture with a changed Cup group assignment is refused and recorded")
    @Test
    void shouldRefuseAndRecordChangedCupGroupAssignment() {
        var baseline = loader.load(new ClassPathResource("seed/test-fixture.yaml"));
        var applied = apply.apply(baseline);
        assertThat(applied).isInstanceOf(ApplyOutcome.Applied.class);

        var altered = loader.load(new ClassPathResource("seed/test-fixture-altered-cup.yaml"));
        var outcome = apply.apply(altered);

        assertThat(outcome).isInstanceOf(ApplyOutcome.RefusedAndRecorded.class);
        var refusal = (ApplyOutcome.RefusedAndRecorded) outcome;

        SeasonStructureId target = baseline.structure().id();
        assertThat(structures.findFingerprint(target))
                .as("stored fingerprint stays on the baseline after a refused apply")
                .contains(refusal.storedFingerprint());

        assertThat(countRefusals()).isEqualTo(1);
        var row = jdbc.sql("""
                        select stored_fingerprint, incoming_fingerprint
                        from refusal_record order by id desc limit 1
                        """)
                .query((rs, rowNum) -> new String[]{
                        rs.getString("stored_fingerprint"), rs.getString("incoming_fingerprint")})
                .single();
        assertThat(row[0]).isEqualTo(refusal.storedFingerprint().value());
        assertThat(row[1]).isEqualTo(refusal.incomingFingerprint().value());
    }

    @DisplayName("Apply on startup is off by default so no structure is written at boot")
    @Test
    void shouldNotApplyOnStartupByDefault() {
        // The Spring context is cached across tests via AbstractIntegrationTest; the database
        // cleaner runs before each test, so after the context has started and this test begins,
        // the structure table must be empty — the startup switch did not apply any seed.
        Integer count = jdbc.sql("select count(*) from season_structure")
                .query(Integer.class).single();
        assertThat(count)
                .as("apply-on-startup must be off by default so no structure is written at boot")
                .isZero();
    }

    private long countRefusals() {
        return jdbc.sql("select count(*) from refusal_record").query(Long.class).single();
    }
}
