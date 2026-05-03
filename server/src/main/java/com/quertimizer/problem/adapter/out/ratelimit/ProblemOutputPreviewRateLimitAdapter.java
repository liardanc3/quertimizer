package com.quertimizer.problem.adapter.out.ratelimit;

import com.quertimizer.auth.application.port.out.AuthRateLimitRepositoryPort;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.application.port.out.ProblemOutputPreviewRateLimitPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ProblemOutputPreviewRateLimitAdapter implements ProblemOutputPreviewRateLimitPort {

    private final AuthRateLimitRepositoryPort authRateLimitRepository;

    @Override
    public void validate(String requester, String clientIp) {
        // 요청자와 클라이언트 IP 조합 기준 짧은 시간 내 반복 호출 제한
        String key = ProblemOutputPreviewRateLimitRules.KEY_PREFIX + ":" + normalize(requester) + ":" + normalize(clientIp);
        Instant now = Instant.now();
        if (authRateLimitRepository.count(key, ProblemOutputPreviewRateLimitRules.WINDOW, now)
                >= ProblemOutputPreviewRateLimitRules.LIMIT) {
            throw new BusinessException(ProblemOutputPreviewRateLimitRules.MESSAGE, HttpStatus.TOO_MANY_REQUESTS);
        }

        authRateLimitRepository.add(key, now);
    }

    private String normalize(String value) {
        // rate limit key에 넣을 수 있도록 빈 식별값을 안전한 기본값으로 정리
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
