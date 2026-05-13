package com.quertimizer.judge.domain.entity;

import com.quertimizer.judge.domain.model.IndexPolicy;
import lombok.Data;

import java.util.List;

@Data
public class SetupSqlDefinition {

    private final JudgeSetupSqlId setupSqlId;
    private final JudgeDatasetId datasetId;
    private final List<String> setupSqls;
    private final IndexPolicy indexPolicy;

    public SetupSqlDefinition(JudgeSetupSqlId setupSqlId,
                              JudgeDatasetId datasetId,
                              List<String> setupSqls,
                              IndexPolicy indexPolicy) {
        this.setupSqlId = setupSqlId;
        this.datasetId = datasetId;
        this.setupSqls = List.copyOf(setupSqls);
        this.indexPolicy = indexPolicy;
    }
}
