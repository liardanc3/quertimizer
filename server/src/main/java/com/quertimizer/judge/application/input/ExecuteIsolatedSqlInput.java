package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import com.quertimizer.judge.domain.model.IsolationPolicy;
import lombok.Data;

import java.util.List;

import static com.quertimizer.judge.domain.model.JudgeFailReason.REQUIRED_FIELD_BLANK;

@Data
public class ExecuteIsolatedSqlInput {

    private final JudgeExecutionId executionId;
    private final JudgeDatasetId datasetId;
    private final List<JudgeSetupSqlId> setupSqlIds;
    private final String targetSql;
    private final IsolationPolicy isolationPolicy;
    private final ExecutionOptions options;

    public ExecuteIsolatedSqlInput(JudgeExecutionId executionId, JudgeDatasetId datasetId,
                                        List<JudgeSetupSqlId> setupSqlIds, String targetSql,
                                        IsolationPolicy isolationPolicy, ExecutionOptions options) {
        this.executionId = executionId;
        this.datasetId = datasetId;
        this.setupSqlIds = List.copyOf(setupSqlIds);
        this.targetSql = requireText(targetSql, "targetSql");
        this.isolationPolicy = isolationPolicy;
        this.options = options;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(REQUIRED_FIELD_BLANK.format(name));
        }

        return value;
    }
}
