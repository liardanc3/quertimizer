package com.quertimizer.monitoring.adapter.out.judge;

import com.quertimizer.judge.adapter.out.execution.LvmSnapshotRuntimeResourceManager;
import com.quertimizer.judge.domain.model.JudgeRuntimeSnapshot;
import com.quertimizer.monitoring.application.output.JudgeRuntimeNodeOutput;
import com.quertimizer.monitoring.application.output.JudgeRuntimeQueueOutput;
import com.quertimizer.monitoring.application.port.out.MonitoringJudgeRuntimePort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JudgeRuntimeMonitoringAdapter implements MonitoringJudgeRuntimePort {

    private final LvmSnapshotRuntimeResourceManager resourceManager;

    public JudgeRuntimeMonitoringAdapter(LvmSnapshotRuntimeResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    public int getTotalWaitingCount() {
        return snapshot().getTotalWaitingCount();
    }

    @Override
    public int getTotalRunningCount() {
        return snapshot().getTotalRunningCount();
    }

    @Override
    public List<JudgeRuntimeQueueOutput> getQueues() {
        return snapshot().getQueues().stream()
                .map(queue -> new JudgeRuntimeQueueOutput(queue.getDbmsType(), queue.getWaitingCount()))
                .toList();
    }

    @Override
    public List<JudgeRuntimeNodeOutput> getNodes() {
        return snapshot().getNodes().stream()
                .map(node -> new JudgeRuntimeNodeOutput(
                        node.getDatabaseId(), node.getDatabaseName(), node.getDbmsType(),
                        node.getRunnerContainer(), node.isEnabled(), node.isReady(),
                        node.getConfiguredMaxConcurrency(), node.getEffectiveMaxConcurrency(),
                        node.getRunningCount(), node.getAvailableRunnerCount(),
                        node.getTotalPortCount(), node.getAvailablePortCount()
                ))
                .toList();
    }

    private JudgeRuntimeSnapshot snapshot() {
        // resource manager의 현재 snapshot 조회
        return resourceManager.createSnapshot();
    }
}
