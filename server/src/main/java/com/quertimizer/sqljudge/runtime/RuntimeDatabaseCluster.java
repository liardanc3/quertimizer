package com.quertimizer.sqljudge.runtime;

import com.quertimizer.sqljudge.db.DbmsType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a runtime database cluster that can select and lease nodes.
 */
public class RuntimeDatabaseCluster {

    private final List<RuntimeDatabase> configuredDatabases;
    private final Map<String, RuntimeDatabasePool> pools;
    private final RuntimeDatabaseSelector selector;

    /**
     * Creates a runtime database cluster with round-robin selection.
     *
     * @param configuredDatabases configured runtime databases
     */
    public RuntimeDatabaseCluster(List<RuntimeDatabase> configuredDatabases) {
        this(configuredDatabases, new RoundRobinRuntimeDatabaseSelector());
    }

    /**
     * Creates a runtime database cluster.
     *
     * @param configuredDatabases configured runtime databases
     * @param selector runtime database selector
     */
    public RuntimeDatabaseCluster(List<RuntimeDatabase> configuredDatabases, RuntimeDatabaseSelector selector) {
        this.configuredDatabases = List.copyOf(Objects.requireNonNull(configuredDatabases, "configuredDatabases must not be null"));
        this.pools = createPools(this.configuredDatabases);
        this.selector = Objects.requireNonNull(selector, "selector must not be null");
    }

    /**
     * Acquires a lease for a ready runtime database matching the DBMS type.
     *
     * @param dbmsType target DBMS type
     * @return runtime database lease
     */
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

    /**
     * Acquires a lease for a specific runtime database node.
     *
     * @param nodeId runtime database node ID
     * @return runtime database lease
     */
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

    /**
     * Returns configured runtime databases.
     *
     * @return configured runtime databases
     */
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
