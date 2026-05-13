package com.quertimizer.monitoring.adapter.in.http.response;

import com.quertimizer.monitoring.application.output.DatabaseStatusOutput;
import lombok.Data;

import java.util.List;

@Data
public class DatabaseStatusRes {

    private final int totalWaitingCount;
    private final int totalRunningCount;
    private final List<DatabaseQueueRes> queues;
    private final List<DatabaseNodeRes> nodes;
    private final List<DockerContainerRes> containers;
    private final List<DatabaseNodeConfigRes> configs;

    public static DatabaseStatusRes from(DatabaseStatusOutput output) {
        return new DatabaseStatusRes(
                output.getTotalWaitingCount(), output.getTotalRunningCount(),
                output.getQueues().stream().map(DatabaseQueueRes::from).toList(),
                output.getNodes().stream().map(DatabaseNodeRes::from).toList(),
                output.getContainers().stream().map(DockerContainerRes::from).toList(),
                output.getConfigs().stream().map(DatabaseNodeConfigRes::from).toList()
        );
    }
}
