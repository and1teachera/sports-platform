package com.sportsplatform.league.domain;

public interface RefusalRecordStore {
    void save(RefusalRecord record);
    long count();
}
