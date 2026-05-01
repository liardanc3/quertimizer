package com.quertimizer.judge.infrastructure.runtime;

import java.util.Objects;
import java.util.concurrent.Semaphore;

public class RuntimeDatabasePool {

    private final RuntimeDatabase database;
    private final Semaphore leases;

    public RuntimeDatabasePool(RuntimeDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
        this.leases = new Semaphore(database.getMaxConcurrency());
    }

    public RuntimeDatabaseLease acquire() {
        if (!leases.tryAcquire()) {
            throw new IllegalStateException("No runtime database lease is available for " + database.getId());
        }

        return new RuntimeDatabaseLease(database, leases::release);
    }

    public boolean hasAvailableLease() {
        return leases.availablePermits() > 0;
    }

    public RuntimeDatabase getDatabase() {
        return database;
    }

    public int getAvailableLeaseCount() {
        return leases.availablePermits();
    }
}
