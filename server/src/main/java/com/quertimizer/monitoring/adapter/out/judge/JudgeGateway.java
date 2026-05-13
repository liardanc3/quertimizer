package com.quertimizer.monitoring.adapter.out.judge;

import com.quertimizer.judge.application.port.in.JudgeApplicationPort;
import com.quertimizer.judge.domain.model.DatabaseSnapshot;
import com.quertimizer.monitoring.application.output.DatabaseNodeOutput;
import com.quertimizer.monitoring.application.output.DatabaseQueueOutput;
import com.quertimizer.monitoring.application.port.out.MonitoringDatabasePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JudgeGateway implements MonitoringDatabasePort {

    private final JudgeApplicationPort judgeApplicationPort;

    @Override
    public int getTotalWaitingCount() {
        return snapshot().getTotalWaitingCount();
    }

    @Override
    public int getTotalRunningCount() {
        return snapshot().getTotalRunningCount();
    }

    @Override
    public List<DatabaseQueueOutput> getQueues() {
        return snapshot().getQueues().stream()
                .map(queue -> new DatabaseQueueOutput(queue.getDbmsType(), queue.getWaitingCount()))
                .toList();
    }

    @Override
    public List<DatabaseNodeOutput> getNodes() {
        return snapshot().getNodes().stream()
                .map(node -> new DatabaseNodeOutput(
                        node.getDatabaseId(), node.getDatabaseName(), node.getDbmsType(),
                        node.getContainerName(), node.isEnabled(), node.isReady(),
                        node.getConfiguredMaxConcurrency(), node.getEffectiveMaxConcurrency(),
                        node.getRunningCount(), node.getAvailableDatabaseCount(),
                        node.getTotalPortCount(), node.getAvailablePortCount()
                ))
                .toList();
    }

    private DatabaseSnapshot snapshot() {
        // DB 실행 환경의 현재 snapshot 조회
        return judgeApplicationPort.createDatabaseSnapshot();
    }
}
