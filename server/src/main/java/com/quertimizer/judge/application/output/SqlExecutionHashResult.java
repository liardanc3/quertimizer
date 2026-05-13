package com.quertimizer.judge.application.output;

import lombok.Data;

@Data
public class SqlExecutionHashResult {

    private final String resultHash;
    private final SqlExecutionResult executionResult;

    public SqlExecutionHashResult(String resultHash, SqlExecutionResult executionResult) {
        this.resultHash = resultHash.trim();
        this.executionResult = executionResult;
    }

}
