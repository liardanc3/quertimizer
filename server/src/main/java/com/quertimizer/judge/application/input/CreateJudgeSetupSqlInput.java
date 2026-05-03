package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.model.IndexPolicy;

import java.util.List;
import java.util.Objects;

public class CreateJudgeSetupSqlInput {

    private final JudgeDatasetId datasetId;
    private final List<String> setupSqls;
    private final IndexPolicy indexPolicy;

    public CreateJudgeSetupSqlInput(JudgeDatasetId datasetId, List<String> setupSqls, IndexPolicy indexPolicy) {
        this.datasetId = Objects.requireNonNull(datasetId, "필수 값이 없다.");
        this.setupSqls = List.copyOf(Objects.requireNonNull(setupSqls, "필수 값이 없다."));
        this.indexPolicy = Objects.requireNonNull(indexPolicy, "필수 값이 없다.");
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public List<String> getSetupSqls() {
        return setupSqls;
    }

    public IndexPolicy getIndexPolicy() {
        return indexPolicy;
    }
}
