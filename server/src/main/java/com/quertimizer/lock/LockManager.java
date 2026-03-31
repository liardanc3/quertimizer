package com.quertimizer.lock;

public interface LockManager {

    boolean tryLock(String key);

    boolean tryLock(String key, long timeoutMillis);

    void lock(String key);

    void unlock(String key);
}
