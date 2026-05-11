package com.quertimizer.monitoring.adapter.in.web.response;

import com.quertimizer.monitoring.application.output.JudgeRuntimeNodeOutput;
import lombok.Data;

@Data
public class JudgeRuntimeNodeRes {

    private final String databaseId;
    private final String databaseName;
    private final String dbmsType;
    private final String dbmsLabel;
    private final String runnerContainer;
    private final boolean enabled;
    private final boolean ready;
    private final int configuredMaxConcurrency;
    private final int effectiveMaxConcurrency;
    private final int runningCount;
    private final int availableRunnerCount;
    private final int totalPortCount;
    private final int availablePortCount;

    public static JudgeRuntimeNodeRes from(JudgeRuntimeNodeOutput output) {
        return new JudgeRuntimeNodeRes(
                output.getDatabaseId(), output.getDatabaseName(),
                output.getDbmsType().getValue(), output.getDbmsType().getLabel(),
                output.getRunnerContainer(), output.isEnabled(), output.isReady(),
                output.getConfiguredMaxConcurrency(), output.getEffectiveMaxConcurrency(),
                output.getRunningCount(), output.getAvailableRunnerCount(),
                output.getTotalPortCount(), output.getAvailablePortCount()
        );
    }
}
