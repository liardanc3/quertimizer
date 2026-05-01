package com.quertimizer.judge.application.output;

import com.quertimizer.judge.domain.entity.ids.JudgeReferenceId;

import java.util.Objects;

public class SqlReferenceResult {

    private final JudgeReferenceId referenceId;
    private final String resultHash;
    private final SqlExecutionResult executionResult;

    public SqlReferenceResult(JudgeReferenceId referenceId, String resultHash, SqlExecutionResult executionResult) {
        this.referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
        this.resultHash = requireText(resultHash, "resultHash");
        this.executionResult = Objects.requireNonNull(executionResult, "executionResult must not be null");
    }

    public JudgeReferenceId getReferenceId() {
        return referenceId;
    }

    public String getResultHash() {
        return resultHash;
    }

    public SqlExecutionResult getExecutionResult() {
        return executionResult;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
