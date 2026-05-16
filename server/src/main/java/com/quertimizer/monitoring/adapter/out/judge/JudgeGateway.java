package com.quertimizer.monitoring.adapter.out.judge;

import com.quertimizer.judge.application.port.in.JudgeApplicationPort;
import com.quertimizer.judge.domain.model.DatabaseSnapshot;
import com.quertimizer.monitoring.application.output.DatabaseNodeOutput;
import com.quertimizer.monitoring.application.output.DatabaseQueueOutput;
import com.quertimizer.monitoring.application.output.MonitoringDatabaseSnapshotOutput;
import com.quertimizer.monitoring.application.port.out.MonitoringDatabasePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JudgeGateway implements MonitoringDatabasePort {

    private final JudgeApplicationPort judgeApplicationPort;

    @Override
    public MonitoringDatabaseSnapshotOutput getSnapshot() {
        DatabaseSnapshot snapshot = judgeApplicationPort.createDatabaseSnapshot();
        List<DatabaseQueueOutput> queues = snapshot.getQueues().stream()
                .map(queue -> new DatabaseQueueOutput(queue.getDbmsType(), queue.getWaitingCount()))
                .toList();
        List<DatabaseNodeOutput> nodes = snapshot.getNodes().stream()
                .map(node -> new DatabaseNodeOutput(
                        node.getDatabaseId(), node.getDatabaseName(), node.getDbmsType(),
                        node.getContainerName(), node.isEnabled(), node.isReady(),
                        node.getConfiguredMaxConcurrency(), node.getEffectiveMaxConcurrency(),
                        node.getRunningCount(), node.getAvailableDatabaseCount(),
                        node.getTotalPortCount(), node.getAvailablePortCount()
                ))
                .toList();
        return new MonitoringDatabaseSnapshotOutput(
                snapshot.getTotalWaitingCount(), snapshot.getTotalRunningCount(),
                queues, nodes
        );
    }
}
