package com.quertimizer.judge.domain.policy;

import com.quertimizer.auth.application.port.AuthRateLimitRepository;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JudgeExecutionRateLimitPolicy {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int LIMIT = 20;

    private final AuthRateLimitRepository authRateLimitRepository;

    public void validate(String requester, String clientIp) {
        String key = "judge-execute:" + normalize(requester) + ":" + normalize(clientIp);
        Instant now = Instant.now();
        if (authRateLimitRepository.count(key, WINDOW, now) >= LIMIT) {
            throw new BusinessException("SQL 실행 요청이 많습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.TOO_MANY_REQUESTS);
        }

        authRateLimitRepository.add(key, now);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
