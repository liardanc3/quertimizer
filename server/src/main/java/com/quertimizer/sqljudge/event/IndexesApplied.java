package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that setup indexes have been applied.
 */
public class IndexesApplied extends AbstractSqlJudgeEvent {

    /**
     * Creates an indexes applied event.
     *
     * @param executionId execution task ID
     */
    public IndexesApplied(JudgeExecutionId executionId) {
        super(executionId);
    }
}
