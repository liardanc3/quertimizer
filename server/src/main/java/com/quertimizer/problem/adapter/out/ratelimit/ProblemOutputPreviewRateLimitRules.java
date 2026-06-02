package com.quertimizer.problem.adapter.out.ratelimit;

import java.time.Duration;

final class ProblemOutputPreviewRateLimitRules {

    static final String KEY_PREFIX = "problem-output-preview";
    static final Duration WINDOW = Duration.ofMinutes(1);
    static final int LIMIT = 40;

    private ProblemOutputPreviewRateLimitRules() {
    }
}
