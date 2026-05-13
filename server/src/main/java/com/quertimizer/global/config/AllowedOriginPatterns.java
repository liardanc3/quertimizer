package com.quertimizer.global.config;

import java.util.List;

public final class AllowedOriginPatterns {

    public static final List<String> VALUES = List.of(
            "http://localhost:*", "http://127.0.0.1:*",
            "https://quertimizer.com", "https://www.quertimizer.com"
    );
    public static final String[] ARRAY = VALUES.toArray(String[]::new);

    private AllowedOriginPatterns() {
    }

}
