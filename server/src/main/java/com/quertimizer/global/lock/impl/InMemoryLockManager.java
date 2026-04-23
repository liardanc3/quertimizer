package com.quertimizer.global.lock.impl;

import com.quertimizer.global.constant.GlobalFailReason;
import com.quertimizer.global.lock.LockManager;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

// Lock을 Memory에서 관리
@Component
public class InMemoryLockManager implements LockManager {

    private final ConcurrentHashMap<String, LockEntry> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key) {
        LockEntry entry = retainEntry(key);
        boolean locked = entry.lock.tryLock();

        if (!locked) {
            releaseEntry(key, entry);
        }

        return locked;
    }

    @Override
    public boolean tryLock(String key, long timeoutMillis) {
        LockEntry entry = retainEntry(key);
        boolean locked = false;

        try {
            locked = entry.lock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);
            return locked;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (!locked) {
                releaseEntry(key, entry);
            }
        }
    }

    @Override
    public void lock(String key) {
        LockEntry entry = retainEntry(key);
        boolean success = false;

        try {
            entry.lock.lock();
            success = true;
        } finally {
            if (!success) {
                releaseEntry(key, entry);
            }
        }
    }

    @Override
    public void unlock(String key) {
        LockEntry entry = locks.get(key);

        if (entry == null) {
            throw new IllegalStateException(GlobalFailReason.LOCK_ENTRY_NOT_FOUND.format(key));
        }

        try {
            entry.lock.unlock();
        } finally {
            releaseEntry(key, entry);
        }
    }

    private LockEntry retainEntry(String key) {
        return locks.compute(key, (ignored, current) -> {
            if (current == null) {
                current = new LockEntry();
            }

            current.references.incrementAndGet();
            return current;
        });
    }

    private void releaseEntry(String key, LockEntry entry) {
        locks.computeIfPresent(key, (ignored, current) -> {
            if (current != entry) {
                return current;
            }

            int remaining = current.references.decrementAndGet();
            if (remaining == 0 && !current.lock.isLocked() && !current.lock.hasQueuedThreads()) {
                return null;
            }

            return current;
        });
    }

    private static final class LockEntry {
        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicInteger references = new AtomicInteger();
    }
}
