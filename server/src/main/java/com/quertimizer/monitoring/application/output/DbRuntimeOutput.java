package com.quertimizer.monitoring.application.output;

import lombok.Data;

import java.util.List;

@Data
public class DbRuntimeOutput {

    private final int totalWaitingCount;
    private final int totalRunningCount;
    private final List<JudgeRuntimeQueueOutput> queues;
    private final List<JudgeRuntimeNodeOutput> nodes;
    private final List<DockerContainerOutput> containers;
    private final List<JudgeConfigOutput> configs;
}
