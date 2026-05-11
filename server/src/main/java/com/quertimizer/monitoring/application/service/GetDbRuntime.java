package com.quertimizer.monitoring.application.service;

import com.quertimizer.monitoring.application.output.DbRuntimeOutput;
import com.quertimizer.monitoring.application.output.JudgeConfigOutput;
import com.quertimizer.monitoring.application.port.in.GetDbRuntimeUseCase;
import com.quertimizer.monitoring.application.port.out.DockerContainerPort;
import com.quertimizer.monitoring.application.port.out.JudgeConfigRepositoryPort;
import com.quertimizer.monitoring.application.port.out.MonitoringJudgeRuntimePort;
import com.quertimizer.monitoring.domain.entity.JudgeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetDbRuntime implements GetDbRuntimeUseCase {

    private final MonitoringJudgeRuntimePort monitoringJudgeRuntimePort;
    private final DockerContainerPort dockerContainerPort;
    private final JudgeConfigRepositoryPort judgeConfigRepositoryPort;

    /**
     * DB runtime 상태를 조회한다.
     *
     * <ol>
     *   <li>런타임 노드와 대기열 상태 조회
     *   <li>docker 컨테이너 상태 조회
     *   <li>동적 judge 설정 합성
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public DbRuntimeOutput execute() {
        Map<String, JudgeConfig> savedConfigsByDatabaseId = judgeConfigRepositoryPort.findAll().stream()
                .collect(Collectors.toMap(JudgeConfig::getDatabaseId, Function.identity()));

        List<JudgeConfigOutput> configs = monitoringJudgeRuntimePort.getNodes().stream()
                .map(node -> savedConfigsByDatabaseId.containsKey(node.getDatabaseId())
                        ? JudgeConfigOutput.from(savedConfigsByDatabaseId.get(node.getDatabaseId()))
                        : new JudgeConfigOutput(
                                node.getDatabaseId(), node.getDatabaseName(), node.getDbmsType(),
                                node.isEnabled(), node.getEffectiveMaxConcurrency(), LocalDateTime.now()
                        ))
                .toList();

        return new DbRuntimeOutput(
                monitoringJudgeRuntimePort.getTotalWaitingCount(),
                monitoringJudgeRuntimePort.getTotalRunningCount(),
                monitoringJudgeRuntimePort.getQueues(),
                monitoringJudgeRuntimePort.getNodes(),
                dockerContainerPort.findJudgeContainers(),
                configs
        );
    }
}
