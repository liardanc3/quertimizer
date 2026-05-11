package com.quertimizer.judge.application.output;

import lombok.Data;

import java.util.Objects;

@Data
public class SqlExecutionHashResult {

    private final String resultHash;
    private final SqlExecutionResult executionResult;

    public SqlExecutionHashResult(String resultHash, SqlExecutionResult executionResult) {
        this.resultHash = requireText(resultHash, "resultHash");
        this.executionResult = Objects.requireNonNull(executionResult, "필수 값이 없습니다.");
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있습니다.");
        }

        return value;
    }
}
