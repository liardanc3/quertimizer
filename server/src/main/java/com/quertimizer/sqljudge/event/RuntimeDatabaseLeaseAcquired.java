package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that a runtime database lease has been acquired.
 */
public class RuntimeDatabaseLeaseAcquired extends AbstractSqlJudgeEvent {

    private final String databaseId;

    /**
     * Creates a runtime database lease acquired event.
     *
     * @param executionId execution task ID
     * @param databaseId leased runtime database ID
     */
    public RuntimeDatabaseLeaseAcquired(JudgeExecutionId executionId, String databaseId) {
        super(executionId);
        if (databaseId == null || databaseId.isBlank()) {
            throw new IllegalArgumentException("databaseId must not be blank");
        }

        this.databaseId = databaseId;
    }

    /**
     * Returns the leased runtime database ID.
     *
     * @return leased runtime database ID
     */
    public String getDatabaseId() {
        return databaseId;
    }
}
