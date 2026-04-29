package com.quertimizer.sqljudge.command;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.policy.EnvironmentPolicy;

import java.util.Objects;

/**
 * Carries input for creating an execution environment from a registered dataset.
 */
public class CreateEnvironmentCommand {

    private final JudgeDatasetId datasetId;
    private final EnvironmentPolicy policy;

    /**
     * Creates an environment creation command.
     *
     * @param datasetId registered dataset ID
     * @param policy environment creation policy
     */
    public CreateEnvironmentCommand(JudgeDatasetId datasetId, EnvironmentPolicy policy) {
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    /**
     * Returns the registered dataset ID.
     *
     * @return registered dataset ID
     */
    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    /**
     * Returns the environment creation policy.
     *
     * @return environment creation policy
     */
    public EnvironmentPolicy getPolicy() {
        return policy;
    }
}
