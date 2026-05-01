package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;

import java.util.Objects;

public class CreateJudgeEnvironmentInput {

    private final JudgeDatasetId datasetId;
    private final EnvironmentPolicy policy;

    public CreateJudgeEnvironmentInput(JudgeDatasetId datasetId, EnvironmentPolicy policy) {
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public EnvironmentPolicy getPolicy() {
        return policy;
    }
}
