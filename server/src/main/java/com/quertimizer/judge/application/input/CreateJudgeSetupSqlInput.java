package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.model.IndexPolicy;

import java.util.List;
import java.util.Objects;

public class CreateJudgeSetupSqlInput {

    private final JudgeDatasetId datasetId;
    private final List<String> setupSqls;
    private final IndexPolicy indexPolicy;

    public CreateJudgeSetupSqlInput(JudgeDatasetId datasetId, List<String> setupSqls, IndexPolicy indexPolicy) {
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.setupSqls = List.copyOf(Objects.requireNonNull(setupSqls, "setupSqls must not be null"));
        this.indexPolicy = Objects.requireNonNull(indexPolicy, "indexPolicy must not be null");
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
