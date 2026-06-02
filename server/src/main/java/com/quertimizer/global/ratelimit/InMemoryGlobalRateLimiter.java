package com.quertimizer.global.ratelimit;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import com.quertimizer.global.exception.GlobalFailReason;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryGlobalRateLimiter {

    private final Map<String, Deque<Instant>> requestsByKey = new ConcurrentHashMap<>();

    public synchronized void validateAndRecord(String shortKey, Duration shortWindow, int shortLimit,
                                               String longKey, Duration longWindow, int longLimit) {
        // 제한 판단에 사용할 window별 요청 기록 정리
        Instant now = Instant.now();
        Deque<Instant> shortRequests = resolveRequests(shortKey, shortWindow, now);
        Deque<Instant> longRequests = resolveRequests(longKey, longWindow, now);

        // 두 window 중 하나라도 제한을 넘으면 요청 차단
        if (shortRequests.size() >= shortLimit || longRequests.size() >= longLimit) {
            throw new DomainRuleViolationException(
                    GlobalFailReason.REQUEST_RATE_LIMIT_DETAIL.getMessage(),
                    DomainRuleViolationType.REQUEST_LIMIT_EXCEEDED
            );
        }

        // 허용된 요청을 두 window에 모두 기록
        shortRequests.addLast(now);
        longRequests.addLast(now);
    }

    private Deque<Instant> resolveRequests(String key, Duration window, Instant now) {
        // key 기준 요청 기록 생성 후 window 밖 기록 제거
        Deque<Instant> requests = requestsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Instant threshold = now.minus(window);
        while (!requests.isEmpty() && requests.peekFirst().isBefore(threshold)) {
            requests.removeFirst();
        }

        return requests;
    }
}
