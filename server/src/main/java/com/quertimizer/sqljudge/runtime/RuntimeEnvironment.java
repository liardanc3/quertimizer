package com.quertimizer.sqljudge.runtime;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeEnvironmentId;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a SQL execution environment managed inside sql-judge.
 */
public class RuntimeEnvironment {

    private final JudgeEnvironmentId environmentId;
    private final JudgeDatasetId datasetId;
    private final RuntimeDatabase database;
    private final RuntimeEnvironmentName name;
    private final Instant createdAt;

    /**
     * Creates a runtime environment.
     *
     * @param environmentId execution environment ID
     * @param datasetId registered dataset ID
     * @param database selected runtime database
     * @param name internal runtime environment name
     * @param createdAt creation time
     */
    public RuntimeEnvironment(JudgeEnvironmentId environmentId,
                              JudgeDatasetId datasetId,
                              RuntimeDatabase database,
                              RuntimeEnvironmentName name,
                              Instant createdAt) {
        this.environmentId = Objects.requireNonNull(environmentId, "environmentId must not be null");
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.database = Objects.requireNonNull(database, "database must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * Returns the execution environment ID.
     *
     * @return execution environment ID
     */
    public JudgeEnvironmentId getEnvironmentId() {
        return environmentId;
    }

    /**
     * Returns the registered dataset ID.
     *
     * @return registered dataset ID
     */
    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    /**
     * Returns the selected runtime database.
     *
     * @return selected runtime database
     */
    public RuntimeDatabase getDatabase() {
        return database;
    }

    /**
     * Returns the internal runtime environment name.
     *
     * @return internal runtime environment name
     */
    public RuntimeEnvironmentName getName() {
        return name;
    }

    /**
     * Returns the creation time.
     *
     * @return creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}
