package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeEnvironmentId;
import com.quertimizer.sqljudge.id.JudgeExecutionId;

import java.util.Objects;

/**
 * Indicates that a SQL execution environment has been created.
 */
public class EnvironmentCreated extends AbstractSqlJudgeEvent {

    private final JudgeEnvironmentId environmentId;

    /**
     * Creates an environment created event.
     *
     * @param executionId execution task ID
     * @param environmentId execution environment ID
     */
    public EnvironmentCreated(JudgeExecutionId executionId, JudgeEnvironmentId environmentId) {
        super(executionId);
        this.environmentId = Objects.requireNonNull(environmentId, "environmentId must not be null");
    }

    /**
     * Returns the execution environment ID.
     *
     * @return execution environment ID
     */
    public JudgeEnvironmentId getEnvironmentId() {
        return environmentId;
    }
}
