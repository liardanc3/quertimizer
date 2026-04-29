package com.quertimizer.sqljudge.result;

import com.quertimizer.sqljudge.id.JudgeReferenceId;

import java.util.Objects;

/**
 * Represents a registered reference SQL result.
 */
public class SqlReferenceResult {

    private final JudgeReferenceId referenceId;
    private final String resultHash;
    private final SqlExecutionResult executionResult;

    /**
     * Creates a registered reference SQL result.
     *
     * @param referenceId registered reference SQL ID
     * @param resultHash canonical SQL result hash
     * @param executionResult SQL execution result
     */
    public SqlReferenceResult(JudgeReferenceId referenceId, String resultHash, SqlExecutionResult executionResult) {
        this.referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
        this.resultHash = requireText(resultHash, "resultHash");
        this.executionResult = Objects.requireNonNull(executionResult, "executionResult must not be null");
    }

    /**
     * Returns the registered reference SQL ID.
     *
     * @return registered reference SQL ID
     */
    public JudgeReferenceId getReferenceId() {
        return referenceId;
    }

    /**
     * Returns the canonical SQL result hash.
     *
     * @return canonical SQL result hash
     */
    public String getResultHash() {
        return resultHash;
    }

    /**
     * Returns the SQL execution result.
     *
     * @return SQL execution result
     */
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
