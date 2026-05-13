package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import lombok.Data;

import static com.quertimizer.judge.domain.model.JudgeFailReason.REQUIRED_TEXT_BLANK;

@Data
public class ExecuteSqlInput {

    private final JudgeExecutionId executionId;
    private final JudgeEnvironmentId environmentId;
    private final String sql;
    private final ExecutionOptions options;

    public ExecuteSqlInput(JudgeExecutionId executionId, JudgeEnvironmentId environmentId,
                                String sql, ExecutionOptions options) {
        this.executionId = executionId;
        this.environmentId = environmentId;
        this.sql = requireSql(sql);
        this.options = options;
    }

    private String requireSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(REQUIRED_TEXT_BLANK.getMessage());
        }

        return sql;
    }
}
