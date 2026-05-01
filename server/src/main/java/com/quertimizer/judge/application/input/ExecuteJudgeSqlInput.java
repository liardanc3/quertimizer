package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.model.ExecutionOptions;

import java.util.Objects;

public class ExecuteJudgeSqlInput {

    private final JudgeExecutionId executionId;
    private final JudgeEnvironmentId environmentId;
    private final String sql;
    private final ExecutionOptions options;

    public ExecuteJudgeSqlInput(JudgeExecutionId executionId,
                             JudgeEnvironmentId environmentId,
                             String sql,
                             ExecutionOptions options) {
        this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
        this.environmentId = Objects.requireNonNull(environmentId, "environmentId must not be null");
        this.sql = requireSql(sql);
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    public JudgeExecutionId getExecutionId() {
        return executionId;
    }

    public JudgeEnvironmentId getEnvironmentId() {
        return environmentId;
    }

    public String getSql() {
        return sql;
    }

    public ExecutionOptions getOptions() {
        return options;
    }

    private String requireSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql must not be blank");
        }

        return sql;
    }
}
