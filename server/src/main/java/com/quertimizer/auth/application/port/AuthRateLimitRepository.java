package com.quertimizer.auth.application.port;

import java.time.Duration;
import java.time.Instant;

public interface AuthRateLimitRepository {

    long count(String key, Instant since);

    void add(String key, Instant at);

    void clear(String key);

    void deleteOlderThan(Instant threshold);

    default long count(String key, Duration window, Instant now) {
        return count(key, now.minus(window));
    }
}
