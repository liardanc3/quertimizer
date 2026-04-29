package com.quertimizer.auth.infrastructure.repository;

import com.quertimizer.auth.application.port.AuthRateLimitRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitInMemoryRepository implements AuthRateLimitRepository {

    private final Map<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    @Override
    public long count(String key, Instant since) {
        Deque<Instant> attempts = attemptsByKey.get(key);
        if (attempts == null) {
            return 0;
        }

        synchronized (attempts) {
            attempts.removeIf(attempt -> attempt.isBefore(since));
            return attempts.size();
        }
    }

    @Override
    public void add(String key, Instant at) {
        Deque<Instant> attempts = attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(at);
        }
    }

    @Override
    public void clear(String key) {
        attemptsByKey.remove(key);
    }

    @Override
    public void deleteOlderThan(Instant threshold) {
        attemptsByKey.values().forEach(attempts -> {
            synchronized (attempts) {
                attempts.removeIf(attempt -> attempt.isBefore(threshold));
            }
        });
        attemptsByKey.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
