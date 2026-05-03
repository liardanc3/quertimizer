package com.quertimizer.judge.domain.entity;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;
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
        this.setupSqlId = Objects.requireNonNull(setupSqlId, "필수 값이 없다.");
        this.datasetId = Objects.requireNonNull(datasetId, "필수 값이 없다.");
        this.setupSqls = List.copyOf(Objects.requireNonNull(setupSqls, "필수 값이 없다."));
        this.indexPolicy = Objects.requireNonNull(indexPolicy, "필수 값이 없다.");
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
