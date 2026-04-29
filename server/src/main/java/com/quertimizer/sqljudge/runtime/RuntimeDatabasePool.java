package com.quertimizer.sqljudge.runtime;

import java.util.Objects;
import java.util.concurrent.Semaphore;

/**
 * Controls concurrent leases for a runtime database.
 */
public class RuntimeDatabasePool {

    private final RuntimeDatabase database;
    private final Semaphore leases;

    /**
     * Creates a runtime database pool.
     *
     * @param database pooled runtime database
     */
    public RuntimeDatabasePool(RuntimeDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        this.leases = new Semaphore(database.getMaxConcurrency());
    }

    /**
     * Acquires a runtime database lease.
     *
     * @return runtime database lease
     */
    public RuntimeDatabaseLease acquire() {
        if (!leases.tryAcquire()) {
            throw new IllegalStateException("No runtime database lease is available for " + database.getId());
        }

        return new RuntimeDatabaseLease(database, leases::release);
    }

    /**
     * Returns whether at least one lease can be acquired.
     *
     * @return true when a lease is available
     */
    public boolean hasAvailableLease() {
        return leases.availablePermits() > 0;
    }

    /**
     * Returns the pooled runtime database.
     *
     * @return pooled runtime database
     */
    public RuntimeDatabase getDatabase() {
        return database;
    }

    /**
     * Returns the number of available leases.
     *
     * @return number of available leases
     */
    public int getAvailableLeaseCount() {
        return leases.availablePermits();
    }
}
