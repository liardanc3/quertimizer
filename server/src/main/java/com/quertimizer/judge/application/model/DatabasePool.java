package com.quertimizer.judge.application.model;

import java.util.concurrent.Semaphore;

import static com.quertimizer.judge.domain.model.JudgeFailReason.RUNTIME_DB_LEASE_UNAVAILABLE;

public class DatabasePool {

    private final Database database;
    private final int maxLeaseCount;
    private final Semaphore leases;

    public DatabasePool(Database database) {
        this(database, database.getMaxConcurrency());
    }

    public DatabasePool(Database database, int maxLeaseCount) {
        this.database = database;
        this.maxLeaseCount = maxLeaseCount;
        this.leases = new Semaphore(this.maxLeaseCount);
    }

    public DatabaseLease acquire() {
        return acquire(database.getMaxConcurrency());
    }

    public synchronized DatabaseLease acquire(int concurrencyLimit) {
        // 동시성 제한과 semaphore 상태 기준 점유 가능 여부 확인
        if (!canAcquire(concurrencyLimit) || !leases.tryAcquire()) {
            throw new IllegalStateException(RUNTIME_DB_LEASE_UNAVAILABLE.format(database.getId()));
        }

        // lease 반환 시 semaphore permit 반환
        return new DatabaseLease(database, leases::release);
    }

    public boolean hasAvailableLease() {
        return hasAvailableLease(database.getMaxConcurrency());
    }

    public synchronized boolean hasAvailableLease(int concurrencyLimit) {
        return canAcquire(concurrencyLimit);
    }

    public Database getDatabase() {
        return database;
    }

    public synchronized int getAvailableLeaseCount(int concurrencyLimit) {
        // 설정 동시성과 pool 크기 중 더 작은 값 기준 가용 lease 계산
        int effectiveLimit = Math.min(concurrencyLimit, maxLeaseCount);
        return Math.max(0, effectiveLimit - getRunningLeaseCount());
    }

    public int getMaxLeaseCount() {
        return maxLeaseCount;
    }

    public int getRunningLeaseCount() {
        return maxLeaseCount - leases.availablePermits();
    }

    private boolean canAcquire(int concurrencyLimit) {
        // 유효 동시성 한도와 semaphore permit 기준 점유 가능 여부 계산
        int effectiveLimit = Math.min(concurrencyLimit, maxLeaseCount);
        return getRunningLeaseCount() < effectiveLimit && leases.availablePermits() > 0;
    }
}
