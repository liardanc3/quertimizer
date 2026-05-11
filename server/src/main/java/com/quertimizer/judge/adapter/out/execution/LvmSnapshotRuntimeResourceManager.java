package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.JudgeQueuePriority;
import com.quertimizer.judge.domain.model.JudgeQueueStatusListener;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LvmSnapshotRuntimeResourceManager {

    private final RuntimeDatabaseCluster databaseCluster;
    private final LvmSnapshotRuntimeOptions options;
    private final Map<String, RuntimeDatabasePool> runnerPools;
    private final Map<String, PortPool> portPools;
    private final ConcurrentHashMap<DbmsType, AtomicInteger> selectionIndexes = new ConcurrentHashMap<>();
    private final LvmSnapshotWorkQueue workQueue = new LvmSnapshotWorkQueue();

    public LvmSnapshotRuntimeResourceManager(RuntimeDatabaseCluster databaseCluster,
                                             LvmSnapshotRuntimeOptions options) {
        this.databaseCluster = Objects.requireNonNull(databaseCluster, "필수 값이 없습니다.");
        this.options = Objects.requireNonNull(options, "필수 값이 없습니다.");
        this.runnerPools = createRunnerPools(databaseCluster.getConfiguredDatabases());
        this.portPools = createPortPools(this.runnerPools.keySet());
    }

    public LvmSnapshotRuntimeSlot acquire(DbmsType dbmsType, JudgeQueuePriority priority,
                                          JudgeQueueStatusListener listener) {
        // Deque 대기열 순번 확보 후 사용 가능한 runner slot 점유
        return workQueue.awaitTurn(dbmsType, priority, listener, () -> acquireAvailableSlot(dbmsType));
    }

    public void release(LvmSnapshotRuntimeSlot slot) {
        // runner 포트와 lease 반환 후 대기열 깨움
        if (slot == null) {
            return;
        }

        RuntimeDatabase runnerDatabase = slot.getRunnerDatabase();
        PortPool portPool = portPools.get(runnerDatabase.getId());
        if (portPool != null) {
            portPool.release(slot.getPort());
        }
        slot.getRunnerLease().close();
        workQueue.notifyAvailableSlot();
    }

    private Optional<LvmSnapshotRuntimeSlot> acquireAvailableSlot(DbmsType dbmsType) {
        // LVM 실행 노드 설정과 후보 runner 조회
        List<RuntimeDatabase> candidates = databaseCluster.getConfiguredDatabases().stream()
                .filter(RuntimeDatabase::isEnabled)
                .filter(database -> database.getDbmsType() == dbmsType)
                .filter(database -> options.findNode(database.getId()).isPresent())
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("LVM 스냅샷 실행 노드 설정이 없습니다: " + dbmsType);
        }

        // runner와 포트를 함께 확보할 수 있는 후보 선택
        AtomicInteger selectionIndex = selectionIndexes.computeIfAbsent(dbmsType, ignored -> new AtomicInteger());
        int startIndex = Math.floorMod(selectionIndex.getAndIncrement(), candidates.size());
        for (int offset = 0; offset < candidates.size(); offset++) {
            RuntimeDatabase candidate = candidates.get(Math.floorMod(startIndex + offset, candidates.size()));
            RuntimeDatabasePool runnerPool = runnerPools.get(candidate.getId());
            PortPool portPool = portPools.get(candidate.getId());
            if (runnerPool != null && portPool != null && runnerPool.hasAvailableLease() && portPool.hasAvailablePort()) {
                RuntimeDatabaseLease lease = runnerPool.acquire();
                int port = portPool.acquire();
                return Optional.of(new LvmSnapshotRuntimeSlot(lease, port));
            }
        }

        return Optional.empty();
    }

    private Map<String, RuntimeDatabasePool> createRunnerPools(List<RuntimeDatabase> databases) {
        // LVM snapshot 대상 runner DB별 lease pool 구성
        Map<String, RuntimeDatabasePool> pools = new LinkedHashMap<>();
        for (RuntimeDatabase database : Objects.requireNonNull(databases, "필수 값이 없습니다.")) {
            if (database.isEnabled() && options.findNode(database.getId()).isPresent()) {
                pools.put(database.getId(), new RuntimeDatabasePool(database));
            }
        }

        return Map.copyOf(pools);
    }

    private Map<String, PortPool> createPortPools(Iterable<String> databaseIds) {
        // runner DB별 per-eval process 포트 pool 구성
        Map<String, PortPool> createdPortPools = new LinkedHashMap<>();
        for (String databaseId : databaseIds) {
            LvmSnapshotRuntimeNode runtimeNode = options.requireNode(databaseId);
            createdPortPools.put(databaseId, new PortPool(runtimeNode.getPortStart(), runtimeNode.getPortEnd()));
        }

        return Map.copyOf(createdPortPools);
    }

    private static final class PortPool {
        private final int portStart;
        private final int portEnd;
        private final Deque<Integer> availablePorts = new ArrayDeque<>();

        private PortPool(int portStart, int portEnd) {
            this.portStart = portStart;
            this.portEnd = portEnd;
            for (int port = portStart; port <= portEnd; port++) {
                availablePorts.addLast(port);
            }
        }

        private synchronized boolean hasAvailablePort() {
            return !availablePorts.isEmpty();
        }

        private synchronized int acquire() {
            Integer port = availablePorts.pollFirst();
            if (port == null) {
                throw new IllegalStateException("사용 가능한 LVM 스냅샷 런타임 포트가 없습니다.");
            }

            return port;
        }

        private synchronized void release(int port) {
            if (port < portStart || port > portEnd || availablePorts.contains(port)) {
                return;
            }

            availablePorts.addLast(port);
        }
    }
}
