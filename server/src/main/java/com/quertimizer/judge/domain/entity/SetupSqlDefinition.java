package com.quertimizer.judge.domain.entity;

import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeSetupSqlId;
import com.quertimizer.judge.domain.model.IndexPolicy;

import java.util.List;
import java.util.Objects;

public class SetupSqlDefinition {

    private final JudgeSetupSqlId setupSqlId;
    private final JudgeDatasetId datasetId;
    private final List<String> setupSqls;
    private final IndexPolicy indexPolicy;

    public SetupSqlDefinition(JudgeSetupSqlId setupSqlId,
                              JudgeDatasetId datasetId,
                              List<String> setupSqls,
                              IndexPolicy indexPolicy) {
        this.setupSqlId = Objects.requireNonNull(setupSqlId, "setupSqlId must not be null");
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.setupSqls = List.copyOf(Objects.requireNonNull(setupSqls, "setupSqls must not be null"));
        this.indexPolicy = Objects.requireNonNull(indexPolicy, "indexPolicy must not be null");
    }

    public JudgeSetupSqlId getSetupSqlId() {
        return setupSqlId;
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
