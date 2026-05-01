package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.model.ExecutionOptions;

import java.util.Objects;

public class CreateJudgeReferenceInput {

    private final JudgeDatasetId datasetId;
    private final String referenceSql;
    private final ExecutionOptions options;

    public CreateJudgeReferenceInput(JudgeDatasetId datasetId, String referenceSql, ExecutionOptions options) {
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.referenceSql = requireText(referenceSql, "referenceSql");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public String getReferenceSql() {
        return referenceSql;
    }

    public ExecutionOptions getOptions() {
        return options;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
