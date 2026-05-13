package com.quertimizer.judge.application.model;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import lombok.Data;

import java.time.Instant;

@Data
public class ExecutionEnvironment {

    private final JudgeEnvironmentId environmentId;
    private final JudgeDatasetId datasetId;
    private final Database database;
    private final EnvironmentName name;
    private final Instant createdAt;

    public ExecutionEnvironment(JudgeEnvironmentId environmentId, JudgeDatasetId datasetId,
                              Database database, EnvironmentName name, Instant createdAt) {
        this.environmentId = environmentId;
        this.datasetId = datasetId;
        this.database = database;
        this.name = name;
        this.createdAt = createdAt;
    }
}
