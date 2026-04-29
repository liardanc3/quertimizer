package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Represents a sql-judge execution event.
 */
public interface SqlJudgeEvent {

    /**
     * Returns the execution task ID.
     *
     * @return execution task ID
     */
    JudgeExecutionId getExecutionId();
}
