package com.fretboard.fretboard.global.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routes queries between the primary and replica datasources based on the current
 * transaction's read-only flag, the current user context, and recent-write marking.
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(RoutingDataSource.class);

    public static final String PRIMARY = "primary";
    public static final String REPLICA = "replica";

    private final ObjectProvider<WriteMarker> writeMarkerProvider;

    public RoutingDataSource(ObjectProvider<WriteMarker> writeMarkerProvider) {
        this.writeMarkerProvider = writeMarkerProvider;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // No actual transaction (boot/Flyway/validate) → primary.
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return PRIMARY;
        }
        // Write transaction → primary.
        if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return PRIMARY;
        }
        // Read-only transaction: anonymous read → replica.
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return REPLICA;
        }
        // Marker not yet available (lazy injection during boot) → safe fallback to primary.
        WriteMarker marker = writeMarkerProvider.getIfAvailable();
        if (marker == null) {
            return PRIMARY;
        }
        try {
            // Read-your-writes: user wrote recently → primary; otherwise replica.
            return marker.hasRecentWrite(userId) ? PRIMARY : REPLICA;
        } catch (RuntimeException e) {
            // Fail-open: consistency may degrade but reads keep working.
            log.warn("Failed to query recent-write marking for userId={}, falling back to replica", userId, e);
            return REPLICA;
        }
    }
}
