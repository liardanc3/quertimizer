package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that a SQL execution has started.
 */
public class ExecutionStarted extends AbstractSqlJudgeEvent {

    /**
     * Creates a SQL execution started event.
     *
     * @param executionId execution task ID
     */
    public ExecutionStarted(JudgeExecutionId executionId) {
        super(executionId);
    }
}
