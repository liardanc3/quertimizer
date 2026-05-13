package com.quertimizer.monitoring.application.output;

import com.quertimizer.judge.application.output.DatabaseNodeConfigOutput;
import lombok.Data;

import java.util.List;

@Data
public class DatabaseStatusOutput {

    private final int totalWaitingCount;
    private final int totalRunningCount;
    private final List<DatabaseQueueOutput> queues;
    private final List<DatabaseNodeOutput> nodes;
    private final List<DockerContainerOutput> containers;
    private final List<DatabaseNodeConfigOutput> configs;
}
