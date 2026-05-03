package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import com.quertimizer.judge.domain.model.IsolationPolicy;

import java.util.List;
import java.util.Objects;

public class ExecuteIsolatedJudgeSqlInput {

    private final JudgeExecutionId executionId;
    private final JudgeDatasetId datasetId;
    private final List<JudgeSetupSqlId> setupSqlIds;
    private final String targetSql;
    private final IsolationPolicy isolationPolicy;
    private final ExecutionOptions options;

    public ExecuteIsolatedJudgeSqlInput(JudgeExecutionId executionId,
                                  JudgeDatasetId datasetId,
                                  List<JudgeSetupSqlId> setupSqlIds,
                                  String targetSql,
                                  IsolationPolicy isolationPolicy,
                                  ExecutionOptions options) {
        this.executionId = Objects.requireNonNull(executionId, "필수 값이 없다.");
        this.datasetId = Objects.requireNonNull(datasetId, "필수 값이 없다.");
        this.setupSqlIds = List.copyOf(Objects.requireNonNull(setupSqlIds, "필수 값이 없다."));
        this.targetSql = requireText(targetSql, "targetSql");
        this.isolationPolicy = Objects.requireNonNull(isolationPolicy, "필수 값이 없다.");
        this.options = Objects.requireNonNull(options, "필수 값이 없다.");
    }

    public JudgeExecutionId getExecutionId() {
        return executionId;
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public List<JudgeSetupSqlId> getSetupSqlIds() {
        return setupSqlIds;
    }

    public String getTargetSql() {
        return targetSql;
    }

    public IsolationPolicy getIsolationPolicy() {
        return isolationPolicy;
    }

    public ExecutionOptions getOptions() {
        return options;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있다.");
        }

        return value;
    }
}
