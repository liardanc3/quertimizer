package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
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
        this.executionId = Objects.requireNonNull(executionId, "필수 값이 없다.");
        this.environmentId = Objects.requireNonNull(environmentId, "필수 값이 없다.");
        this.sql = requireSql(sql);
        this.options = Objects.requireNonNull(options, "필수 값이 없다.");
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
            throw new IllegalArgumentException("필수 문자열이 비어 있다.");
        }

        return sql;
    }
}
