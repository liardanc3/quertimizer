package com.quertimizer.sqljudge.command;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.policy.ExecutionOptions;

import java.util.Objects;

/**
 * Carries reference SQL source material for registering a reusable reference definition.
 */
public class CreateReferenceCommand {

    private final JudgeDatasetId datasetId;
    private final String referenceSql;
    private final ExecutionOptions options;

    /**
     * Creates a reference SQL registration command.
     *
     * @param datasetId registered dataset ID
     * @param referenceSql reference SQL statement
     * @param options SQL execution options
     */
    public CreateReferenceCommand(JudgeDatasetId datasetId, String referenceSql, ExecutionOptions options) {
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.referenceSql = requireText(referenceSql, "referenceSql");
        this.options = Objects.requireNonNull(options, "options must not be null");
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
     * Returns the reference SQL statement.
     *
     * @return reference SQL statement
     */
    public String getReferenceSql() {
        return referenceSql;
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
