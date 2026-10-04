package com.sportsplatform.support;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Truncates every user table in the {@code public} schema, leaving Flyway's own history alone.
 * One {@code TRUNCATE ... CASCADE} statement, so it survives V2 adding tables.
 */
public final class DatabaseCleaner {

    private final JdbcClient jdbc;

    public DatabaseCleaner(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void clean() {
        List<String> tables = jdbc.sql("""
                        select table_name from information_schema.tables
                        where table_schema = 'public'
                        and table_type = 'BASE TABLE'
                        and table_name <> 'flyway_schema_history'
                        """)
                .query(String.class)
                .list();
        if (tables.isEmpty()) return;
        String quoted = tables.stream().map(t -> "\"" + t + "\"").collect(Collectors.joining(", "));
        jdbc.sql("truncate table " + quoted + " restart identity cascade").update();
    }
}
