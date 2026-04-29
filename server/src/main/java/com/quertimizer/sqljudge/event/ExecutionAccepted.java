package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that a SQL execution has been accepted.
 */
public class ExecutionAccepted extends AbstractSqlJudgeEvent {

    /**
     * Creates a SQL execution accepted event.
     *
     * @param executionId execution task ID
     */
    public ExecutionAccepted(JudgeExecutionId executionId) {
        super(executionId);
    }
}
