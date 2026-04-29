package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

import java.util.Objects;

/**
 * Provides common execution ID handling for sql-judge events.
 */
public abstract class AbstractSqlJudgeEvent implements SqlJudgeEvent {

    private final JudgeExecutionId executionId;

    /**
     * Creates a sql-judge event.
     *
     * @param executionId execution task ID
     */
    protected AbstractSqlJudgeEvent(JudgeExecutionId executionId) {
        this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
    }

    /**
     * Returns the execution task ID.
     *
     * @return execution task ID
     */
    @Override
    public JudgeExecutionId getExecutionId() {
        return executionId;
    }
}
