package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;

import java.util.Objects;

public class CreateJudgeEnvironmentInput {

    private final JudgeDatasetId datasetId;
    private final EnvironmentPolicy policy;

    public CreateJudgeEnvironmentInput(JudgeDatasetId datasetId, EnvironmentPolicy policy) {
        this.datasetId = Objects.requireNonNull(datasetId, "필수 값이 없다.");
        this.policy = Objects.requireNonNull(policy, "필수 값이 없다.");
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public EnvironmentPolicy getPolicy() {
        return policy;
    }
}
