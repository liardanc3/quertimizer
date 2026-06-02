package com.quertimizer.global.ratelimit;

import java.time.Duration;

public final class GlobalRateLimitRules {

    public static final Duration HTTP_SHORT_WINDOW = Duration.ofSeconds(10);
    public static final Duration HTTP_LONG_WINDOW = Duration.ofMinutes(1);
    public static final int HTTP_AUTHENTICATED_SHORT_LIMIT = 72;
    public static final int HTTP_AUTHENTICATED_LONG_LIMIT = 288;
    public static final int HTTP_ANONYMOUS_SHORT_LIMIT = 96;
    public static final int HTTP_ANONYMOUS_LONG_LIMIT = 360;
    public static final int HTTP_ADMIN_SHORT_LIMIT = 144;
    public static final int HTTP_ADMIN_LONG_LIMIT = 720;

    public static final Duration WEBSOCKET_SHORT_WINDOW = Duration.ofSeconds(10);
    public static final Duration WEBSOCKET_LONG_WINDOW = Duration.ofMinutes(1);
    public static final int WEBSOCKET_AUTHENTICATED_SHORT_LIMIT = 10;
    public static final int WEBSOCKET_AUTHENTICATED_LONG_LIMIT = 36;
    public static final int WEBSOCKET_ANONYMOUS_SHORT_LIMIT = 10;
    public static final int WEBSOCKET_ANONYMOUS_LONG_LIMIT = 36;
    public static final int WEBSOCKET_ADMIN_SHORT_LIMIT = 24;
    public static final int WEBSOCKET_ADMIN_LONG_LIMIT = 144;

    private GlobalRateLimitRules() {
    }
}
