package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that a SQL execution has been cancelled.
 */
public class ExecutionCancelled extends AbstractSqlJudgeEvent {

    /**
     * Creates a SQL execution cancelled event.
     *
     * @param executionId execution task ID
     */
    public ExecutionCancelled(JudgeExecutionId executionId) {
        super(executionId);
    }
}
