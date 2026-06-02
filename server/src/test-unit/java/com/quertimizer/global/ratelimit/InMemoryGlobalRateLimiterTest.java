package com.quertimizer.global.ratelimit;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryGlobalRateLimiter")
class InMemoryGlobalRateLimiterTest {

    private final InMemoryGlobalRateLimiter rateLimiter = new InMemoryGlobalRateLimiter();

    @Nested
    @DisplayName("validateAndRecord")
    class ValidateAndRecord {

        @Test
        @DisplayName("성공 (제한 미만)")
        void successWhenRequestCountBelowLimit() {
            // given
            String shortKey = "http:short:user:solver";
            String longKey = "http:long:user:solver";

            // when
            rateLimiter.validateAndRecord(shortKey, Duration.ofSeconds(10), 2, longKey, Duration.ofMinutes(1), 2);

            // then
        }

        @Test
        @DisplayName("실패 (짧은 window 제한 이상)")
        void failWhenShortWindowLimitExceeded() {
            // given
            String shortKey = "ws:short:user:solver";
            String longKey = "ws:long:user:solver";
            rateLimiter.validateAndRecord(shortKey, Duration.ofSeconds(10), 1, longKey, Duration.ofMinutes(1), 10);

            // when & then
            assertThatThrownBy(() -> rateLimiter.validateAndRecord(shortKey, Duration.ofSeconds(10), 1, longKey, Duration.ofMinutes(1), 10))
                    .isInstanceOf(DomainRuleViolationException.class)
                    .extracting("type")
                    .isEqualTo(DomainRuleViolationType.REQUEST_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("실패 (긴 window 제한 이상)")
        void failWhenLongWindowLimitExceeded() {
            // given
            String shortKey = "http:short:ip:127.0.0.1";
            String longKey = "http:long:ip:127.0.0.1";
            rateLimiter.validateAndRecord(shortKey, Duration.ofSeconds(10), 10, longKey, Duration.ofMinutes(1), 1);

            // when & then
            assertThatThrownBy(() -> rateLimiter.validateAndRecord(shortKey, Duration.ofSeconds(10), 10, longKey, Duration.ofMinutes(1), 1))
                    .isInstanceOf(DomainRuleViolationException.class)
                    .extracting("type")
                    .isEqualTo(DomainRuleViolationType.REQUEST_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("성공 (다른 key 분리)")
        void successWhenKeysAreDifferent() {
            // given
            rateLimiter.validateAndRecord("ws:short:user:a", Duration.ofSeconds(10), 1, "ws:long:user:a", Duration.ofMinutes(1), 1);

            // when
            rateLimiter.validateAndRecord("ws:short:user:b", Duration.ofSeconds(10), 1, "ws:long:user:b", Duration.ofMinutes(1), 1);

            // then
        }
    }
}
