package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import lombok.Data;

import static com.quertimizer.judge.domain.model.JudgeFailReason.REQUIRED_FIELD_BLANK;

@Data
public class CreateSqlExecutionHashInput {

    private final JudgeDatasetId datasetId;
    private final String sql;
    private final ExecutionOptions options;

    public CreateSqlExecutionHashInput(JudgeDatasetId datasetId, String sql, ExecutionOptions options) {
        this.datasetId = datasetId;
        this.sql = requireText(sql, "sql");
        this.options = options;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(REQUIRED_FIELD_BLANK.format(name));
        }

        return value;
    }
}
