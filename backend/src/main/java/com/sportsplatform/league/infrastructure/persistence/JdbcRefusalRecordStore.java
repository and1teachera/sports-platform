package com.sportsplatform.league.infrastructure.persistence;

import com.sportsplatform.league.domain.RefusalRecord;
import com.sportsplatform.league.domain.RefusalRecordStore;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class JdbcRefusalRecordStore implements RefusalRecordStore {

    private final JdbcClient jdbc;

    public JdbcRefusalRecordStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(RefusalRecord record) {
        jdbc.sql("""
                        insert into refusal_record
                        (league_id, season_id, stored_fingerprint, incoming_fingerprint, detected_at)
                        values (?, ?, ?, ?, ?)
                        """)
                .param(record.target().league().value())
                .param(record.target().season().value())
                .param(record.storedFingerprint().value())
                .param(record.incomingFingerprint().value())
                .param(Timestamp.from(record.detectedAt()))
                .update();
    }
}
