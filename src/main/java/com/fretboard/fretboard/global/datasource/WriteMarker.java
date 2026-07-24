package com.fretboard.fretboard.global.datasource;

/**
 * Marks and queries whether a given user has performed a recent write.
 *
 * <p>Used by {@link RoutingDataSource} to route a read-only transaction to the primary
 * datasource when the user has a recent write, avoiding read-after-write inconsistency
 * caused by primary/replica replication lag.
 *
 * <p>The real Redis-backed implementation is provided by a later task; for now this is
 * a minimal contract so routing logic can depend on it.
 */
public interface WriteMarker {

    boolean hasRecentWrite(Long userId);
}
