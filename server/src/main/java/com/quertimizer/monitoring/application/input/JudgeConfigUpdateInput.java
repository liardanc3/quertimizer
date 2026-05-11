package com.quertimizer.monitoring.application.input;

import lombok.Data;

@Data
public class JudgeConfigUpdateInput {

    private final String databaseId;
    private final boolean enabled;
    private final int maxConcurrency;
}
