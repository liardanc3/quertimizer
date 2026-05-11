package com.quertimizer.monitoring.domain.model;

import java.util.Arrays;

public enum MonitoringLogLevel {
    DEBUG("debug"),
    INFO("info"),
    WARN("warn");

    private final String value;

    MonitoringLogLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        // 로그 레벨 저장값 조회
        return value;
    }

    public static MonitoringLogLevel fromValueOrDefault(String value, MonitoringLogLevel fallback) {
        // 문자열 기준 로그 레벨 조회
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return Arrays.stream(values())
                .filter(level -> level.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(fallback);
    }
}
