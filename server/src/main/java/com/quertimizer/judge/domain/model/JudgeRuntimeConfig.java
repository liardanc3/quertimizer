package com.quertimizer.judge.domain.model;

import lombok.Data;

@Data
public class JudgeRuntimeConfig {

    private final String databaseId;
    private final boolean enabled;
    private final int maxConcurrency;

    public static JudgeRuntimeConfig defaultConfig(String databaseId, boolean enabled, int maxConcurrency) {
        // 기본 설정 기준 런타임 설정 생성
        return new JudgeRuntimeConfig(databaseId, enabled, maxConcurrency);
    }
}
