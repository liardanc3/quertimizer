package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;
import com.quertimizer.sqljudge.result.SqlExecutionResult;

import java.util.Objects;

/**
 * Indicates that a SQL execution has completed.
 */
public class ExecutionCompleted extends AbstractSqlJudgeEvent {

    private final SqlExecutionResult result;

    /**
     * Creates a SQL execution completion event.
     *
     * @param executionId execution task ID
     * @param result SQL execution result
     */
    public ExecutionCompleted(JudgeExecutionId executionId, SqlExecutionResult result) {
        super(executionId);
        this.result = Objects.requireNonNull(result, "result must not be null");
    }

    /**
     * Returns the SQL execution result.
     *
     * @return SQL execution result
     */
    public SqlExecutionResult getResult() {
        return result;
    }
}
