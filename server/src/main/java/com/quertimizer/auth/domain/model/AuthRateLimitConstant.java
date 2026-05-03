package com.quertimizer.auth.domain.model;

import java.time.Duration;

public final class AuthRateLimitConstant {

    public static final Duration LOGIN_LOCK_WINDOW = Duration.ofMinutes(10);
    public static final int LOGIN_FAILURE_LIMIT = 10;
    public static final Duration CODE_MINUTE_WINDOW = Duration.ofMinutes(1);
    public static final Duration CODE_HOUR_WINDOW = Duration.ofHours(1);
    public static final int CODE_MINUTE_LIMIT = 20;
    public static final int CODE_HOUR_LIMIT = 100;
    public static final Duration CODE_VERIFY_WINDOW = Duration.ofMinutes(10);
    public static final int CODE_VERIFY_FAILURE_LIMIT = 5;
    public static final Duration PASSWORD_RESET_WINDOW = Duration.ofMinutes(10);
    public static final int PASSWORD_RESET_LIMIT = 3;

    private AuthRateLimitConstant() {
    }
}
