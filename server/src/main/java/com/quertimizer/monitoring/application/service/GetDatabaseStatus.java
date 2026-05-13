package com.quertimizer.monitoring.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.monitoring.application.output.DatabaseStatusOutput;
import com.quertimizer.monitoring.application.port.in.GetDatabaseStatusUseCase;
import com.quertimizer.monitoring.application.port.out.DockerContainerPort;
import com.quertimizer.monitoring.application.port.out.MonitoringDatabasePort;
import com.quertimizer.judge.application.output.DatabaseNodeConfigOutput;
import com.quertimizer.judge.application.port.out.DatabaseNodeConfigRepositoryPort;
import com.quertimizer.judge.domain.entity.DatabaseNodeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetDatabaseStatus implements GetDatabaseStatusUseCase {

    private final MonitoringDatabasePort monitoringDatabasePort;
    private final DockerContainerPort dockerContainerPort;
    private final DatabaseNodeConfigRepositoryPort databaseNodeConfigRepositoryPort;

    /**
     * DB 실행 환경 상태를 조회한다.
     *
     * <ol>
     *   <li>DB 실행 환경 노드와 대기열 상태 조회
     *   <li>docker 컨테이너 상태 조회
     *   <li>동적 DB 실행 환경 설정 합성
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public DatabaseStatusOutput execute() {
        Map<String, DatabaseNodeConfig> savedConfigsByDatabaseId = databaseNodeConfigRepositoryPort.findAll().stream()
                .collect(Collectors.toMap(DatabaseNodeConfig::getDatabaseId, Function.identity()));

        List<DatabaseNodeConfigOutput> configs = monitoringDatabasePort.getNodes().stream()
                .map(node -> savedConfigsByDatabaseId.get(node.getDatabaseId()))
                .filter(config -> config != null)
                .map(DatabaseNodeConfigOutput::from)
                .toList();

        return new DatabaseStatusOutput(
                monitoringDatabasePort.getTotalWaitingCount(),
                monitoringDatabasePort.getTotalRunningCount(),
                monitoringDatabasePort.getQueues(),
                monitoringDatabasePort.getNodes(),
                dockerContainerPort.findJudgeContainers(),
                configs
        );
    }
}
