package com.quertimizer.monitoring.application.output;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

@Data
public class JudgeRuntimeNodeOutput {

    private final String databaseId;
    private final String databaseName;
    private final DbmsType dbmsType;
    private final String runnerContainer;
    private final boolean enabled;
    private final boolean ready;
    private final int configuredMaxConcurrency;
    private final int effectiveMaxConcurrency;
    private final int runningCount;
    private final int availableRunnerCount;
    private final int totalPortCount;
    private final int availablePortCount;
}
