package com.quertimizer.problem.infrastructure.ratelimit;

import java.time.Duration;

final class ProblemOutputPreviewRateLimitRules {

    static final String KEY_PREFIX = "problem-output-preview";
    static final String MESSAGE = "SQL 실행 요청이 많습니다. 잠시 후 다시 시도해 주세요.";
    static final Duration WINDOW = Duration.ofMinutes(1);
    static final int LIMIT = 20;

    private ProblemOutputPreviewRateLimitRules() {
    }
}
