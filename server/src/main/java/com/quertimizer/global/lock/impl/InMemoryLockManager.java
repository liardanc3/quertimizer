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
        // 즉시 점유하지 못한 락 엔트리 참조 수 복원
        LockEntry entry = retainEntry(key);
        boolean locked = entry.lock.tryLock();

        if (!locked) {
            releaseEntry(key, entry);
        }

        return locked;
    }

    @Override
    public boolean tryLock(String key, long timeoutMillis) {
        // 제한 시간 안에 점유하지 못한 락 엔트리 참조 수 복원
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
        // 블로킹 락 점유 실패 시 생성 참조 정리
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
        // 점유 해제 후 미사용 락 엔트리 제거
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
        // 락 엔트리 유지 여부 확인
        return locks.compute(key, (ignored, current) -> {
            if (current == null) {
                current = new LockEntry();
            }

            current.references.incrementAndGet();
            return current;
        });
    }

    private void releaseEntry(String key, LockEntry entry) {
        // 참조와 대기자가 모두 사라진 락 엔트리만 map에서 제거
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
