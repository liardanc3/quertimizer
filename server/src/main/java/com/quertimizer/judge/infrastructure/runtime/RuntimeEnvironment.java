package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;

import java.time.Instant;
import java.util.Objects;

public class RuntimeEnvironment {

    private final JudgeEnvironmentId environmentId;
    private final JudgeDatasetId datasetId;
    private final RuntimeDatabase database;
    private final RuntimeEnvironmentName name;
    private final Instant createdAt;

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

    public JudgeEnvironmentId getEnvironmentId() {
        return environmentId;
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public RuntimeDatabase getDatabase() {
        return database;
    }

    public RuntimeEnvironmentName getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
