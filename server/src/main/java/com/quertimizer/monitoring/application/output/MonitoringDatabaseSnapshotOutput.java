package com.quertimizer.monitoring.application.output;

import lombok.Data;

import java.util.List;

@Data
public class MonitoringDatabaseSnapshotOutput {

    private final int totalWaitingCount;
    private final int totalRunningCount;
    private final List<DatabaseQueueOutput> queues;
    private final List<DatabaseNodeOutput> nodes;
}
