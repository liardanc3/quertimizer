package com.quertimizer.judge.adapter.out.lvm;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.application.port.out.LvmSnapshotPort;
import com.quertimizer.judge.application.port.out.DatabaseNodeConfigRepositoryPort;
import com.quertimizer.judge.application.port.out.DatabaseSnapshotPort;
import com.quertimizer.judge.application.model.Database;
import com.quertimizer.judge.application.model.DatabaseCluster;
import com.quertimizer.judge.application.model.DatabaseLease;
import com.quertimizer.judge.application.model.DatabasePool;
import com.quertimizer.judge.application.model.DatabaseNode;
import com.quertimizer.judge.application.model.Options;
import com.quertimizer.judge.application.model.DatabaseSlot;
import com.quertimizer.judge.domain.entity.DatabaseNodeConfig;
import com.quertimizer.judge.domain.model.QueuePriority;
import com.quertimizer.judge.domain.model.QueueStatusListener;
import com.quertimizer.judge.domain.model.DatabaseNodeSnapshot;
import com.quertimizer.judge.domain.model.DatabaseQueueSnapshot;
import com.quertimizer.judge.domain.model.DatabaseSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_SNAPSHOT_NODE_CONFIG_NOT_FOUND;
import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_SNAPSHOT_PORT_UNAVAILABLE;
import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_SNAPSHOT_WAIT_INTERRUPTED;

@Component
public class LvmSnapshotService implements LvmSnapshotPort, DatabaseSnapshotPort {

    private static final long WAIT_REPORT_INTERVAL_MILLIS = 1000L;

    private final DatabaseCluster databaseCluster;
    private final Options options;
    private final LvmSnapshotCommandFactory lvmSnapshotCommandFactory;
    private final LvmSnapshotExecutor lvmSnapshotExecutor;
    private final DatabaseNodeConfigRepositoryPort databaseNodeConfigRepositoryPort;
    private final Map<String, DatabasePool> databasePools;
    private final Map<String, PortPool> portPools;
    private final ConcurrentHashMap<DbmsType, AtomicInteger> selectionIndexes = new ConcurrentHashMap<>();
    private final WorkQueue workQueue = new WorkQueue();

    public LvmSnapshotService(DatabaseCluster databaseCluster, Options options,
                              LvmSnapshotCommandFactory lvmSnapshotCommandFactory,
                              LvmSnapshotExecutor lvmSnapshotExecutor,
                              DatabaseNodeConfigRepositoryPort databaseNodeConfigRepositoryPort) {
        this.databaseCluster = databaseCluster;
        this.options = options;
        this.lvmSnapshotCommandFactory = lvmSnapshotCommandFactory;
        this.lvmSnapshotExecutor = lvmSnapshotExecutor;
        this.databaseNodeConfigRepositoryPort = databaseNodeConfigRepositoryPort;
        this.databasePools = createDatabasePools(databaseCluster.getConfiguredDatabases());
        this.portPools = createPortPools(this.databasePools.keySet());
    }

    @Override
    public DatabaseSlot acquire(DbmsType dbmsType, QueuePriority priority,
                                     QueueStatusListener listener) {
        // Deque 대기열 순번 확보 후 사용 가능한 DB slot 점유
        return workQueue.awaitTurn(dbmsType, priority, listener, () -> acquireAvailableSlot(dbmsType));
    }

    @Override
    public void release(DatabaseSlot slot) {
        // DB 포트와 lease 반환 후 대기열 깨움
        if (slot == null) {
            return;
        }

        Database database = slot.getDatabase();
        PortPool portPool = portPools.get(database.getId());
        if (portPool != null) {
            portPool.release(slot.getPort());
        }
        slot.getDatabaseLease().close();
        workQueue.notifyAvailableSlot();
    }

    public void createMaintenanceTemplate(String scriptDbmsName, String templateVersion) {
        // 기준 템플릿 기반 유지보수 snapshot 생성
        lvmSnapshotExecutor.executeAll(lvmSnapshotCommandFactory.createMaintenanceTemplateCommands(
                scriptDbmsName, options.getBaseTemplateVersion(), templateVersion
        ));
    }

    public void prepareTemplateLog(String scriptDbmsName, String templateVersion) {
        // 템플릿 DB 프로세스 로그 경로 준비
        lvmSnapshotExecutor.executeAll(lvmSnapshotCommandFactory.prepareTemplateLogCommands(scriptDbmsName, templateVersion));
    }

    public void sealTemplate(String scriptDbmsName, String templateVersion) {
        // 데이터 적재가 끝난 템플릿 snapshot 봉인
        lvmSnapshotExecutor.executeAll(lvmSnapshotCommandFactory.sealTemplateCommands(scriptDbmsName, templateVersion));
    }

    public void dropTemplate(String scriptDbmsName, String templateVersion) {
        // 데이터셋 템플릿 snapshot 제거
        lvmSnapshotExecutor.executeAll(lvmSnapshotCommandFactory.dropTemplateCommands(scriptDbmsName, templateVersion));
    }

    public String evalLvName(String scriptDbmsName, String environmentScriptName) {
        // 평가 LV 이름 생성
        return lvmSnapshotCommandFactory.evalLvNameForEnvironment(scriptDbmsName, environmentScriptName);
    }

    public String listEvalSnapshotNames() {
        // 평가 LV 이름 목록 조회
        return lvmSnapshotExecutor.execute(lvmSnapshotCommandFactory.listEvalSnapshotNamesCommand());
    }

    public void createEvalSnapshot(String scriptDbmsName, String templateVersion, String environmentScriptName) {
        // 읽기 전용 템플릿 기반 평가 snapshot 생성
        lvmSnapshotExecutor.executeAll(lvmSnapshotCommandFactory.createEvalSnapshotCommands(
                scriptDbmsName, templateVersion, environmentScriptName
        ));
    }

    public void dropEvalSnapshot(String scriptDbmsName, String environmentScriptName) {
        // 평가 snapshot 제거
        lvmSnapshotExecutor.executeAll(lvmSnapshotCommandFactory.dropEvalSnapshotCommands(scriptDbmsName, environmentScriptName));
    }

    public DbmsType resolveEvalDbmsType(String evalLvName) {
        // 평가 LV 이름 기준 DBMS 유형 변환
        return lvmSnapshotCommandFactory.resolveEvalDbmsType(evalLvName);
    }

    public String resolveEvalEnvironmentName(String evalLvName) {
        // 평가 LV 이름 기준 실행 환경 스크립트 이름 추출
        return lvmSnapshotCommandFactory.resolveEvalEnvironmentName(evalLvName);
    }

    public void dropOrphanEvalSnapshot(String evalLvName) {
        // 고아 평가 snapshot 제거
        lvmSnapshotExecutor.execute(lvmSnapshotCommandFactory.dropOrphanEvalSnapshotCommand(evalLvName));
    }

    @Override
    public DatabaseSnapshot createSnapshot() {
        // DB 노드별 현재 점유 상태와 포트 상태 수집
        List<DatabaseNodeSnapshot> nodes = databaseCluster.getConfiguredDatabases().stream()
                .map(this::createNodeSnapshot)
                .toList();

        // DBMS별 대기열 상태와 전체 처리 상태 집계
        Map<DbmsType, Integer> waitingCountByDbmsType = workQueue.countWaitingByDbmsType();
        List<DatabaseQueueSnapshot> queues = waitingCountByDbmsType.entrySet().stream()
                .map(entry -> new DatabaseQueueSnapshot(entry.getKey(), entry.getValue()))
                .toList();
        int totalWaitingCount = waitingCountByDbmsType.values().stream().mapToInt(Integer::intValue).sum();
        int totalRunningCount = nodes.stream().mapToInt(DatabaseNodeSnapshot::getRunningCount).sum();

        return new DatabaseSnapshot(nodes, queues, totalWaitingCount, totalRunningCount);
    }

    private Optional<DatabaseSlot> acquireAvailableSlot(DbmsType dbmsType) {
        // LVM 실행 노드 설정과 후보 DB 노드 조회
        List<Database> candidates = databaseCluster.getConfiguredDatabases().stream()
                .filter(this::isDatabaseEnabled)
                .filter(database -> database.getDbmsType() == dbmsType)
                .filter(database -> options.findNode(database.getId()).isPresent())
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException(LVM_SNAPSHOT_NODE_CONFIG_NOT_FOUND.format(dbmsType));
        }

        // DB 노드와 포트를 함께 확보할 수 있는 후보 선택
        AtomicInteger selectionIndex = selectionIndexes.computeIfAbsent(dbmsType, ignored -> new AtomicInteger());
        int startIndex = Math.floorMod(selectionIndex.getAndIncrement(), candidates.size());
        for (int offset = 0; offset < candidates.size(); offset++) {
            Database candidate = candidates.get(Math.floorMod(startIndex + offset, candidates.size()));
            DatabasePool databasePool = databasePools.get(candidate.getId());
            PortPool portPool = portPools.get(candidate.getId());
            int effectiveMaxConcurrency = resolveEffectiveMaxConcurrency(candidate, databasePool, portPool);
            if (databasePool != null && portPool != null && effectiveMaxConcurrency > 0
                    && databasePool.hasAvailableLease(effectiveMaxConcurrency) && portPool.hasAvailablePort()) {
                DatabaseLease lease = databasePool.acquire(effectiveMaxConcurrency);
                int port = portPool.acquire();
                return Optional.of(new DatabaseSlot(lease, port));
            }
        }

        return Optional.empty();
    }

    private DatabaseNodeSnapshot createNodeSnapshot(Database database) {
        // DB 노드 설정과 실제 점유 pool 상태 조회
        DatabaseNode databaseNode = options.findNode(database.getId()).orElse(null);
        DatabasePool databasePool = databasePools.get(database.getId());
        PortPool portPool = portPools.get(database.getId());

        // 모니터링 응답에 필요한 동시 실행 가능 수와 현재 실행 수 계산
        int configuredMaxConcurrency = resolveConfiguredMaxConcurrency(database);
        int totalPortCount = portPool != null ? portPool.getTotalPortCount() : 0;
        int availablePortCount = portPool != null ? portPool.getAvailablePortCount() : 0;
        int effectiveMaxConcurrency = resolveEffectiveMaxConcurrency(database, databasePool, portPool);
        int availableDatabaseCount = databasePool != null && effectiveMaxConcurrency > 0
                ? databasePool.getAvailableLeaseCount(effectiveMaxConcurrency)
                : 0;
        int runningCount = databasePool != null ? databasePool.getRunningLeaseCount() : 0;

        return new DatabaseNodeSnapshot(
                database.getId(), database.getName(), database.getDbmsType(),
                databaseNode != null ? databaseNode.getContainerName() : "",
                isDatabaseEnabled(database), database.isReady() && databaseNode != null && isDatabaseEnabled(database),
                configuredMaxConcurrency, effectiveMaxConcurrency,
                runningCount, availableDatabaseCount,
                totalPortCount, availablePortCount
        );
    }

    private Map<String, DatabasePool> createDatabasePools(List<Database> databases) {
        // LVM snapshot 대상 DB 노드별 lease pool 구성
        Map<String, DatabasePool> pools = new LinkedHashMap<>();
        for (Database database : databases) {
            if (database.isEnabled() && options.findNode(database.getId()).isPresent()) {
                DatabaseNode databaseNode = options.requireNode(database.getId());
                int maxLeaseCount = databaseNode.getPortEnd() - databaseNode.getPortStart() + 1;
                pools.put(database.getId(), new DatabasePool(database, maxLeaseCount));
            }
        }

        return Map.copyOf(pools);
    }

    private boolean isDatabaseEnabled(Database database) {
        // 저장된 DB 노드 설정이 있으면 우선 적용하고 없으면 애플리케이션 설정 사용
        return databaseNodeConfigRepositoryPort.findByDatabaseId(database.getId())
                .map(DatabaseNodeConfig::isEnabled)
                .orElse(database.isEnabled());
    }

    private int resolveConfiguredMaxConcurrency(Database database) {
        // 저장된 동시 실행 수가 있으면 우선 적용하고 없으면 애플리케이션 설정 사용
        return databaseNodeConfigRepositoryPort.findByDatabaseId(database.getId())
                .map(DatabaseNodeConfig::getMaxConcurrency)
                .orElse(database.getMaxConcurrency());
    }

    private int resolveEffectiveMaxConcurrency(Database database, DatabasePool databasePool, PortPool portPool) {
        // DB pool과 포트 범위를 넘지 않는 실제 동시 실행 수 계산
        int configuredMaxConcurrency = resolveConfiguredMaxConcurrency(database);
        int maxLeaseCount = databasePool != null ? databasePool.getMaxLeaseCount() : 0;
        int totalPortCount = portPool != null ? portPool.getTotalPortCount() : 0;
        return Math.max(0, Math.min(configuredMaxConcurrency, Math.min(maxLeaseCount, totalPortCount)));
    }

    private Map<String, PortPool> createPortPools(Iterable<String> databaseIds) {
        // DB 노드별 per-eval process 포트 pool 구성
        Map<String, PortPool> createdPortPools = new LinkedHashMap<>();
        for (String databaseId : databaseIds) {
            DatabaseNode databaseNode = options.requireNode(databaseId);
            createdPortPools.put(databaseId, new PortPool(databaseNode.getPortStart(), databaseNode.getPortEnd()));
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
                throw new IllegalStateException(LVM_SNAPSHOT_PORT_UNAVAILABLE.getMessage());
            }

            return port;
        }

        private synchronized int getTotalPortCount() {
            return portEnd - portStart + 1;
        }

        private synchronized int getAvailablePortCount() {
            return availablePorts.size();
        }

        private synchronized void release(int port) {
            if (port < portStart || port > portEnd || availablePorts.contains(port)) {
                return;
            }

            availablePorts.addLast(port);
        }
    }

    private static final class WorkQueue {
        private final Object monitor = new Object();
        private final Deque<QueueTicket> queue = new ArrayDeque<>();

        private DatabaseSlot awaitTurn(DbmsType dbmsType, QueuePriority priority,
                                      QueueStatusListener listener,
                                      Supplier<Optional<DatabaseSlot>> slotSupplier) {
            // 대기열 ticket 등록
            QueueTicket ticket = new QueueTicket(dbmsType, priority);
            QueueStatusListener statusListener = listener != null ? listener : QueueStatusListener.noop();
            synchronized (monitor) {
                if (priority == QueuePriority.FIRST) {
                    addPriorityTicket(ticket);
                } else {
                    queue.addLast(ticket);
                }
                monitor.notifyAll();
            }

            // 순번과 DB slot 확보까지 대기
            while (true) {
                int remainingTasks;
                synchronized (monitor) {
                    try {
                        if (isFirstForDbms(ticket)) {
                            Optional<DatabaseSlot> slot = slotSupplier.get();
                            if (slot.isPresent()) {
                                queue.remove(ticket);
                                monitor.notifyAll();
                                return slot.get();
                            }
                        }
                    } catch (RuntimeException exception) {
                        queue.remove(ticket);
                        monitor.notifyAll();
                        throw exception;
                    }
                    remainingTasks = countBefore(ticket);
                }

                // 현재 ticket 앞의 작업 수 전달
                statusListener.onWaiting(remainingTasks);
                synchronized (monitor) {
                    try {
                        monitor.wait(WAIT_REPORT_INTERVAL_MILLIS);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        queue.remove(ticket);
                        monitor.notifyAll();
                        throw new IllegalStateException(LVM_SNAPSHOT_WAIT_INTERRUPTED.getMessage(), exception);
                    }
                }
            }
        }

        private void notifyAvailableSlot() {
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }

        private Map<DbmsType, Integer> countWaitingByDbmsType() {
            // 현재 대기열을 DBMS별 대기 작업 수로 집계
            Map<DbmsType, Integer> waitingCountByDbmsType = new LinkedHashMap<>();
            synchronized (monitor) {
                for (QueueTicket ticket : queue) {
                    waitingCountByDbmsType.merge(ticket.dbmsType, 1, Integer::sum);
                }
            }

            return Map.copyOf(waitingCountByDbmsType);
        }

        private void addPriorityTicket(QueueTicket ticket) {
            // 기존 우선 작업 뒤와 일반 작업 앞 사이에 ticket 삽입
            Deque<QueueTicket> reorderedQueue = new ArrayDeque<>();
            boolean added = false;
            while (!queue.isEmpty()) {
                QueueTicket queuedTicket = queue.removeFirst();
                if (!added && queuedTicket.priority != QueuePriority.FIRST) {
                    reorderedQueue.addLast(ticket);
                    added = true;
                }
                reorderedQueue.addLast(queuedTicket);
            }
            if (!added) {
                reorderedQueue.addLast(ticket);
            }
            queue.addAll(reorderedQueue);
        }

        private boolean isFirstForDbms(QueueTicket ticket) {
            // 같은 DBMS 기준 선두 ticket 여부 확인
            for (QueueTicket queuedTicket : queue) {
                if (queuedTicket.dbmsType == ticket.dbmsType) {
                    return queuedTicket.equals(ticket);
                }
            }

            return false;
        }

        private int countBefore(QueueTicket ticket) {
            // 같은 DBMS 기준 현재 ticket 앞 대기 작업 수 계산
            int count = 0;
            for (QueueTicket queuedTicket : queue) {
                if (queuedTicket.equals(ticket)) {
                    return count;
                }
                if (queuedTicket.dbmsType == ticket.dbmsType) {
                    count++;
                }
            }

            return count;
        }
    }

    private static final class QueueTicket {
        private final String id = UUID.randomUUID().toString();
        private final DbmsType dbmsType;
        private final QueuePriority priority;

        private QueueTicket(DbmsType dbmsType, QueuePriority priority) {
            this.dbmsType = dbmsType;
            this.priority = priority;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof QueueTicket ticket)) {
                return false;
            }

            return id.equals(ticket.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
