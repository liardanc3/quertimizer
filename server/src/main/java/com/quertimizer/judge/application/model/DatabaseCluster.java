package com.quertimizer.judge.application.model;

import com.quertimizer.judge.domain.model.DbmsType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.quertimizer.judge.domain.model.JudgeFailReason.RUNTIME_DB_CONFIG_NOT_READY;
import static com.quertimizer.judge.domain.model.JudgeFailReason.RUNTIME_DB_LEASE_UNAVAILABLE;
import static com.quertimizer.judge.domain.model.JudgeFailReason.RUNTIME_DB_NODE_NOT_READY;
import static com.quertimizer.judge.domain.model.JudgeFailReason.UNKNOWN_RUNTIME_DB_NODE;

public class DatabaseCluster {

    private final List<Database> configuredDatabases;
    private final Map<String, DatabasePool> pools;
    private final DatabaseSelector selector;

    public DatabaseCluster(List<Database> configuredDatabases, DatabaseSelector selector) {
        this.configuredDatabases = List.copyOf(configuredDatabases);
        this.pools = createPools(this.configuredDatabases);
        this.selector = selector;
    }

    public DatabaseLease acquire(DbmsType dbmsType) {
        // DBMS 기준 준비된 DB 노드 후보 조회
        List<Database> candidates = configuredDatabases.stream()
                .filter(Database::isReady)
                .filter(database -> database.getDbmsType() == dbmsType)
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException(RUNTIME_DB_CONFIG_NOT_READY.format(dbmsType));
        }

        // selector 시작 지점부터 사용 가능한 lease 탐색
        int startIndex = selector.selectStartIndex(candidates);
        for (int offset = 0; offset < candidates.size(); offset++) {
            Database database = candidates.get(Math.floorMod(startIndex + offset, candidates.size()));
            DatabasePool pool = pools.get(database.getId());
            if (pool.hasAvailableLease()) {
                return pool.acquire();
            }
        }

        throw new IllegalStateException(RUNTIME_DB_LEASE_UNAVAILABLE.format(dbmsType));
    }

    public DatabaseLease acquireNode(String nodeId) {
        // DB 노드 노드 ID 기준 pool 조회
        DatabasePool pool = pools.get(nodeId);
        if (pool == null) {
            throw new IllegalArgumentException(UNKNOWN_RUNTIME_DB_NODE.format(nodeId));
        }

        // 비활성 DB 노드 노드 점유 차단
        if (!pool.getDatabase().isReady()) {
            throw new IllegalStateException(RUNTIME_DB_NODE_NOT_READY.format(nodeId));
        }

        // DB 노드 노드 pool 점유
        return pool.acquire();
    }

    public List<Database> getConfiguredDatabases() {
        return configuredDatabases;
    }

    private Map<String, DatabasePool> createPools(List<Database> databases) {
        // DB 노드별 lease pool 구성
        Map<String, DatabasePool> createdPools = new LinkedHashMap<>();
        for (Database database : databases) {
            createdPools.put(database.getId(), new DatabasePool(database));
        }

        // 외부 변경을 막는 불변 map 반환
        return Collections.unmodifiableMap(createdPools);
    }
}
