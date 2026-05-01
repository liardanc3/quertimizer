package com.quertimizer.judge.infrastructure.runtime;

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
        this.configuredDatabases = List.copyOf(Objects.requireNonNull(configuredDatabases, "configuredDatabases must not be null"));
        this.pools = createPools(this.configuredDatabases);
        this.selector = Objects.requireNonNull(selector, "selector must not be null");
    }

    public RuntimeDatabaseLease acquire(DbmsType dbmsType) {
        Objects.requireNonNull(dbmsType, "dbmsType must not be null");

        List<RuntimeDatabase> candidates = configuredDatabases.stream()
                .filter(RuntimeDatabase::isReady)
                .filter(database -> database.getDbmsType() == dbmsType)
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No ready runtime database is configured for " + dbmsType);
        }

        int startIndex = selector.selectStartIndex(candidates);
        for (int offset = 0; offset < candidates.size(); offset++) {
            RuntimeDatabase database = candidates.get(Math.floorMod(startIndex + offset, candidates.size()));
            RuntimeDatabasePool pool = pools.get(database.getId());
            if (pool.hasAvailableLease()) {
                return pool.acquire();
            }
        }

        throw new IllegalStateException("No runtime database lease is available for " + dbmsType);
    }

    public RuntimeDatabaseLease acquireNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }

        RuntimeDatabasePool pool = pools.get(nodeId);
        if (pool == null) {
            throw new IllegalArgumentException("Unknown runtime database node: " + nodeId);
        }

        if (!pool.getDatabase().isReady()) {
            throw new IllegalStateException("Runtime database node is not ready: " + nodeId);
        }

        return pool.acquire();
    }

    public List<RuntimeDatabase> getConfiguredDatabases() {
        return configuredDatabases;
    }

    private Map<String, RuntimeDatabasePool> createPools(List<RuntimeDatabase> databases) {
        Map<String, RuntimeDatabasePool> createdPools = new LinkedHashMap<>();
        for (RuntimeDatabase database : databases) {
            Objects.requireNonNull(database, "database must not be null");

            if (createdPools.containsKey(database.getId())) {
                throw new IllegalArgumentException("Runtime database IDs must be unique: " + database.getId());
            }

            createdPools.put(database.getId(), new RuntimeDatabasePool(database));
        }

        return Collections.unmodifiableMap(createdPools);
    }
}
