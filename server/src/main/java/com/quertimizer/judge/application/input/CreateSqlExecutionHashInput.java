package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.model.ExecutionOptions;

import java.util.Objects;

public class CreateSqlExecutionHashInput {

    private final JudgeDatasetId datasetId;
    private final String sql;
    private final ExecutionOptions options;

    public CreateSqlExecutionHashInput(JudgeDatasetId datasetId, String sql, ExecutionOptions options) {
        this.datasetId = Objects.requireNonNull(datasetId, "필수 값이 없습니다.");
        this.sql = requireText(sql, "sql");
        this.options = Objects.requireNonNull(options, "필수 값이 없습니다.");
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public String getSql() {
        return sql;
    }

    public ExecutionOptions getOptions() {
        return options;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있습니다.");
        }

        return value;
    }
}
