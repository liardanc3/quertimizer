package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.judge.application.port.JudgeDatabaseClusterPort;
import com.quertimizer.judge.application.port.JudgeDatabaseNodePort;
import com.quertimizer.judge.infrastructure.config.JudgeDatabaseProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JudgeDatabaseCluster implements JudgeDatabaseClusterPort {

    private final Map<DbmsType, List<JudgeDatabaseNode>> nodesByEngine = new EnumMap<>(DbmsType.class);
    private final Map<String, JudgeDatabaseNode> nodesById = new HashMap<>();
    private final RoundRobinJudgeDatabaseSelector judgeDatabaseSelector;
    private final Duration leaseAcquireTimeout;

    public JudgeDatabaseCluster(JudgeDatabaseProperties judgeDatabaseProperties,
                                JudgeDatabaseConnectionProvider connectionProvider,
                                RoundRobinJudgeDatabaseSelector judgeDatabaseSelector) {
        this.judgeDatabaseSelector = judgeDatabaseSelector;
        this.leaseAcquireTimeout = judgeDatabaseProperties.getLeaseAcquireTimeout();

        int nodeIndex = 0;
        for (JudgeDatabaseProperties.DatabaseProperties properties : judgeDatabaseProperties.getDatabases()) {
            DbmsType engine = properties.resolveEngine()
                    .orElseThrow(() -> new IllegalStateException("judge.databases engine 값이 올바르지 않다."));
            JudgeDatabaseNode node = createNode(properties, engine, nodeIndex++, connectionProvider);
            nodesByEngine.computeIfAbsent(engine, key -> new ArrayList<>()).add(node);
            nodesById.put(node.getId(), node);
        }
    }

    @Override
    public synchronized JudgeDatabaseLease acquire(DbmsType engine) {
        // DBMS 엔진 기준으로 사용 가능한 judge DB node를 점유
        List<JudgeDatabaseNode> nodes = getReadyNodes(engine);
        if (nodes.isEmpty()) {
            throw new IllegalStateException("%s judge DB node 설정이 0개다.".formatted(engine.getValue()));
        }

        return waitAndAcquire(nodes, "%s judge DB node 대기 중 인터럽트가 발생했다.".formatted(engine.getValue()));
    }

    @Override
    public synchronized JudgeDatabaseLease acquireNode(String nodeId) {
        // 지정된 judge DB node를 점유
        JudgeDatabaseNode node = nodesById.get(nodeId);
        if (node == null || !node.isReady()) {
            throw new IllegalStateException("사용 가능한 judge DB node가 없다: " + nodeId);
        }

        return waitAndAcquire(List.of(node), "judge DB node 대기 중 인터럽트가 발생했다: " + nodeId);
    }

    public synchronized void release(JudgeDatabaseLease lease) {
        // 외부에서 전달받은 lease를 반환
        lease.close();
    }

    public List<JudgeDatabaseNode> getReadyNodes(DbmsType engine) {
        // 엔진별 사용 가능한 node 목록 조회
        return nodesByEngine.getOrDefault(engine, List.of()).stream()
                .filter(JudgeDatabaseNode::isReady)
                .toList();
    }

    @Override
    public List<JudgeDatabaseNodePort> getConfiguredNodes() {
        // 설정된 전체 judge DB node 목록 조회
        return nodesById.values().stream()
                .map(JudgeDatabaseNodePort.class::cast)
                .toList();
    }

    private JudgeDatabaseLease waitAndAcquire(List<JudgeDatabaseNode> nodes, String interruptedMessage) {
        Instant deadline = Instant.now().plus(leaseAcquireTimeout);
        while (true) {
            int startIndex = judgeDatabaseSelector.selectStartIndex(nodes);
            for (int offset = 0; offset < nodes.size(); offset++) {
                JudgeDatabaseNode node = nodes.get((startIndex + offset) % nodes.size());
                if (node.tryAcquire()) {
                    return new JudgeDatabaseLease(node, () -> releaseNode(node));
                }
            }

            if (!Instant.now().isBefore(deadline)) {
                throw new BusinessException("사용 가능한 judge 실행 환경이 없습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            try {
                wait(Math.min(200L, Math.max(1L, Duration.between(Instant.now(), deadline).toMillis())));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interruptedMessage, exception);
            }
        }
    }

    private synchronized void releaseNode(JudgeDatabaseNode node) {
        // 점유한 node를 반환하고 대기 중인 요청을 깨운다
        node.release();
        notifyAll();
    }

    private JudgeDatabaseNode createNode(JudgeDatabaseProperties.DatabaseProperties properties,
                                         DbmsType engine,
                                         int nodeIndex,
                                         JudgeDatabaseConnectionProvider connectionProvider) {
        String nodeId = resolveNodeId(properties, engine, nodeIndex);
        return new JudgeDatabaseNode(
                nodeId,
                !isBlank(properties.getName()) ? properties.getName().trim() : nodeId,
                engine,
                normalize(properties.getUrl()),
                normalize(properties.getUsername()),
                properties.getPassword(),
                properties.isEnabled(),
                properties.getMaxConcurrency(),
                properties.getWeight() != null ? properties.getWeight() : 1,
                connectionProvider
        );
    }

    private String resolveNodeId(JudgeDatabaseProperties.DatabaseProperties properties, DbmsType engine, int nodeIndex) {
        // node id가 비어 있으면 엔진과 순번으로 안정적인 id를 만든다
        if (!isBlank(properties.getId())) {
            return properties.getId().trim();
        }

        if (!isBlank(properties.getName())) {
            return "%s-%s".formatted(engine.getValue(), properties.getName().trim());
        }

        return "%s-node-%d".formatted(engine.getValue(), nodeIndex + 1);
    }

    private String normalize(String value) {
        return value != null ? value.trim() : "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
