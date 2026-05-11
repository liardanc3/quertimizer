package com.quertimizer.monitoring.adapter.in.web.response;

import com.quertimizer.monitoring.application.output.DbRuntimeOutput;
import lombok.Data;

import java.util.List;

@Data
public class DbRuntimeRes {

    private final int totalWaitingCount;
    private final int totalRunningCount;
    private final List<JudgeRuntimeQueueRes> queues;
    private final List<JudgeRuntimeNodeRes> nodes;
    private final List<DockerContainerRes> containers;
    private final List<JudgeConfigRes> configs;

    public static DbRuntimeRes from(DbRuntimeOutput output) {
        return new DbRuntimeRes(
                output.getTotalWaitingCount(), output.getTotalRunningCount(),
                output.getQueues().stream().map(JudgeRuntimeQueueRes::from).toList(),
                output.getNodes().stream().map(JudgeRuntimeNodeRes::from).toList(),
                output.getContainers().stream().map(DockerContainerRes::from).toList(),
                output.getConfigs().stream().map(JudgeConfigRes::from).toList()
        );
    }
}
