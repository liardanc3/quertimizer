package com.quertimizer.judge.application.input;

import lombok.Data;

@Data
public class DatabaseNodeConfigUpdateInput {

    private final String databaseId;
    private final boolean enabled;
    private final int maxConcurrency;
}
