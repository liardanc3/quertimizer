package com.quertimizer.monitoring.adapter.in.http.response;

import com.quertimizer.monitoring.application.output.DatabaseNodeOutput;
import lombok.Data;

@Data
public class DatabaseNodeRes {

    private final String databaseId;
    private final String databaseName;
    private final String dbmsType;
    private final String dbmsLabel;
    private final String containerName;
    private final boolean enabled;
    private final boolean ready;
    private final int configuredMaxConcurrency;
    private final int effectiveMaxConcurrency;
    private final int runningCount;
    private final int availableDatabaseCount;
    private final int totalPortCount;
    private final int availablePortCount;

    public static DatabaseNodeRes from(DatabaseNodeOutput output) {
        return new DatabaseNodeRes(
                output.getDatabaseId(), output.getDatabaseName(),
                output.getDbmsType().getValue(), output.getDbmsType().getLabel(),
                output.getContainerName(), output.isEnabled(), output.isReady(),
                output.getConfiguredMaxConcurrency(), output.getEffectiveMaxConcurrency(),
                output.getRunningCount(), output.getAvailableDatabaseCount(),
                output.getTotalPortCount(), output.getAvailablePortCount()
        );
    }
}
