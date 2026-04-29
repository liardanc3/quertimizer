package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that runtime database statistics have been initialized.
 */
public class StatisticsInitialized extends AbstractSqlJudgeEvent {

    /**
     * Creates a statistics initialized event.
     *
     * @param executionId execution task ID
     */
    public StatisticsInitialized(JudgeExecutionId executionId) {
        super(executionId);
    }
}
