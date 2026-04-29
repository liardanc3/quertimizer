package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that a runtime database has been selected.
 */
public class RuntimeDatabaseSelected extends AbstractSqlJudgeEvent {

    private final String databaseId;

    /**
     * Creates a runtime database selected event.
     *
     * @param executionId execution task ID
     * @param databaseId selected runtime database ID
     */
    public RuntimeDatabaseSelected(JudgeExecutionId executionId, String databaseId) {
        super(executionId);
        if (databaseId == null || databaseId.isBlank()) {
            throw new IllegalArgumentException("databaseId must not be blank");
        }

        this.databaseId = databaseId;
    }

    /**
     * Returns the selected runtime database ID.
     *
     * @return selected runtime database ID
     */
    public String getDatabaseId() {
        return databaseId;
    }
}
