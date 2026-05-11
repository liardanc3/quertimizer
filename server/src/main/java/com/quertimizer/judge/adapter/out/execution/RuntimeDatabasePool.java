package com.quertimizer.judge.adapter.out.execution;

import java.util.Objects;
import java.util.concurrent.Semaphore;

public class RuntimeDatabasePool {

    private final RuntimeDatabase database;
    private final Semaphore leases;

    public RuntimeDatabasePool(RuntimeDatabase database) {
        this.database = Objects.requireNonNull(database, "필수 값이 없습니다.");
        this.leases = new Semaphore(database.getMaxConcurrency());
    }

    public RuntimeDatabaseLease acquire() {
        if (!leases.tryAcquire()) {
            throw new IllegalStateException("사용 가능한 런타임 DB 점유가 없습니다: " + database.getId());
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
