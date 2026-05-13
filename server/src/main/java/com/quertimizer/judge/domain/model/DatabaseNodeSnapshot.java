package com.quertimizer.judge.domain.model;

import com.quertimizer.judge.domain.model.DbmsType;

import lombok.Data;

@Data
public class DatabaseNodeSnapshot {

    private final String databaseId;
    private final String databaseName;
    private final DbmsType dbmsType;
    private final String containerName;
    private final boolean enabled;
    private final boolean ready;
    private final int configuredMaxConcurrency;
    private final int effectiveMaxConcurrency;
    private final int runningCount;
    private final int availableDatabaseCount;
    private final int totalPortCount;
    private final int availablePortCount;
}
