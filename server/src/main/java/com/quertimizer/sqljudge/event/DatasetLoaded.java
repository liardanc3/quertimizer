package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that dataset SQL has been loaded.
 */
public class DatasetLoaded extends AbstractSqlJudgeEvent {

    /**
     * Creates a dataset loaded event.
     *
     * @param executionId execution task ID
     */
    public DatasetLoaded(JudgeExecutionId executionId) {
        super(executionId);
    }
}
