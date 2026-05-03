package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.model.DbmsType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RuntimeDatabaseCluster {

    private final List<RuntimeDatabase> configuredDatabases;
    private final Map<String, RuntimeDatabasePool> pools;
    private final RuntimeDatabaseSelector selector;

    public RuntimeDatabaseCluster(List<RuntimeDatabase> configuredDatabases) {
        this(configuredDatabases, new RoundRobinRuntimeDatabaseSelector());
    }

    public RuntimeDatabaseCluster(List<RuntimeDatabase> configuredDatabases, RuntimeDatabaseSelector selector) {
        this.configuredDatabases = List.copyOf(Objects.requireNonNull(configuredDatabases, "필수 값이 없다."));
        this.pools = createPools(this.configuredDatabases);
        this.selector = Objects.requireNonNull(selector, "필수 값이 없다.");
    }

    public RuntimeDatabaseLease acquire(DbmsType dbmsType) {
        Objects.requireNonNull(dbmsType, "필수 값이 없다.");

        List<RuntimeDatabase> candidates = configuredDatabases.stream()
                .filter(RuntimeDatabase::isReady)
                .filter(database -> database.getDbmsType() == dbmsType)
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("준비된 런타임 DB 설정이 없다: " + dbmsType);
        }

        int startIndex = selector.selectStartIndex(candidates);
        for (int offset = 0; offset < candidates.size(); offset++) {
            RuntimeDatabase database = candidates.get(Math.floorMod(startIndex + offset, candidates.size()));
            RuntimeDatabasePool pool = pools.get(database.getId());
            if (pool.hasAvailableLease()) {
                return pool.acquire();
            }
        }

        throw new IllegalStateException("사용 가능한 런타임 DB 점유가 없다: " + dbmsType);
    }

    public RuntimeDatabaseLease acquireNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("필수 문자열이 비어 있다.");
        }

        RuntimeDatabasePool pool = pools.get(nodeId);
        if (pool == null) {
            throw new IllegalArgumentException("알 수 없는 런타임 DB 노드: " + nodeId);
        }

        if (!pool.getDatabase().isReady()) {
            throw new IllegalStateException("런타임 DB 노드가 준비되지 않았다: " + nodeId);
        }

        return pool.acquire();
    }

    public List<RuntimeDatabase> getConfiguredDatabases() {
        return configuredDatabases;
    }

    private Map<String, RuntimeDatabasePool> createPools(List<RuntimeDatabase> databases) {
        Map<String, RuntimeDatabasePool> createdPools = new LinkedHashMap<>();
        for (RuntimeDatabase database : databases) {
            Objects.requireNonNull(database, "필수 값이 없다.");

            if (createdPools.containsKey(database.getId())) {
                throw new IllegalArgumentException("런타임 DB ID가 중복됐다: " + database.getId());
            }

            createdPools.put(database.getId(), new RuntimeDatabasePool(database));
        }

        return Collections.unmodifiableMap(createdPools);
    }
}
