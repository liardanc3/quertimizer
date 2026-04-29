package com.quertimizer.sqljudge.command;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeExecutionId;
import com.quertimizer.sqljudge.id.JudgeSetupSqlId;
import com.quertimizer.sqljudge.policy.ExecutionOptions;
import com.quertimizer.sqljudge.policy.IsolationPolicy;

import java.util.List;
import java.util.Objects;

/**
 * Carries SQL execution input for a clean isolated environment.
 */
public class IsolatedExecuteCommand {

    private final JudgeExecutionId executionId;
    private final JudgeDatasetId datasetId;
    private final List<JudgeSetupSqlId> setupSqlIds;
    private final String targetSql;
    private final IsolationPolicy isolationPolicy;
    private final ExecutionOptions options;

    /**
     * Creates an isolated SQL execution command.
     *
     * @param executionId execution task ID
     * @param datasetId registered dataset ID
     * @param setupSqlIds registered setup SQL bundle IDs
     * @param targetSql SQL statement to execute
     * @param isolationPolicy isolated execution policy
     * @param options SQL execution options
     */
    public IsolatedExecuteCommand(JudgeExecutionId executionId,
                                  JudgeDatasetId datasetId,
                                  List<JudgeSetupSqlId> setupSqlIds,
                                  String targetSql,
                                  IsolationPolicy isolationPolicy,
                                  ExecutionOptions options) {
        this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.setupSqlIds = List.copyOf(Objects.requireNonNull(setupSqlIds, "setupSqlIds must not be null"));
        this.targetSql = requireText(targetSql, "targetSql");
        this.isolationPolicy = Objects.requireNonNull(isolationPolicy, "isolationPolicy must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Returns the execution task ID.
     *
     * @return execution task ID
     */
    public JudgeExecutionId getExecutionId() {
        return executionId;
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
     * Returns the registered setup SQL bundle IDs.
     *
     * @return registered setup SQL bundle IDs
     */
    public List<JudgeSetupSqlId> getSetupSqlIds() {
        return setupSqlIds;
    }

    /**
     * Returns the SQL statement to execute.
     *
     * @return SQL statement to execute
     */
    public String getTargetSql() {
        return targetSql;
    }

    /**
     * Returns the isolated execution policy.
     *
     * @return isolated execution policy
     */
    public IsolationPolicy getIsolationPolicy() {
        return isolationPolicy;
    }

    /**
     * Returns the SQL execution options.
     *
     * @return SQL execution options
     */
    public ExecutionOptions getOptions() {
        return options;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
