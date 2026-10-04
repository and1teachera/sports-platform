package com.sportsplatform.league.domain;

import java.util.function.Supplier;

/**
 * Domain port for atomic multi-table work. The infrastructure adapter binds this to a real
 * database transaction; the application layer uses the port without knowing about transactions
 * or JDBC.
 */
public interface UnitOfWork {
    <T> T execute(Supplier<T> work);
}
