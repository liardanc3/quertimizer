package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;

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
        this.environmentId = Objects.requireNonNull(environmentId, "필수 값이 없다.");
        this.datasetId = Objects.requireNonNull(datasetId, "필수 값이 없다.");
        this.database = Objects.requireNonNull(database, "필수 값이 없다.");
        this.name = Objects.requireNonNull(name, "필수 값이 없다.");
        this.createdAt = Objects.requireNonNull(createdAt, "필수 값이 없다.");
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
