package com.quertimizer.sqljudge.command;

import com.quertimizer.sqljudge.id.JudgeEnvironmentId;

import java.util.Objects;

/**
 * Carries input for dropping a SQL execution environment.
 */
public class DropEnvironmentCommand {

    private final JudgeEnvironmentId environmentId;

    /**
     * Creates an environment drop command.
     *
     * @param environmentId execution environment ID
     */
    public DropEnvironmentCommand(JudgeEnvironmentId environmentId) {
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
