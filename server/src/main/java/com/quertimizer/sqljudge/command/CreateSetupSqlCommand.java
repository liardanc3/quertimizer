package com.quertimizer.sqljudge.command;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.policy.IndexPolicy;

import java.util.List;
import java.util.Objects;

/**
 * Carries setup SQL source material for registering a reusable setup SQL bundle.
 */
public class CreateSetupSqlCommand {

    private final JudgeDatasetId datasetId;
    private final List<String> setupSqls;
    private final IndexPolicy indexPolicy;

    /**
     * Creates a setup SQL registration command.
     *
     * @param datasetId dataset ID that the setup SQL bundle targets
     * @param setupSqls setup SQL statements
     * @param indexPolicy index handling policy
     */
    public CreateSetupSqlCommand(JudgeDatasetId datasetId, List<String> setupSqls, IndexPolicy indexPolicy) {
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.setupSqls = List.copyOf(Objects.requireNonNull(setupSqls, "setupSqls must not be null"));
        this.indexPolicy = Objects.requireNonNull(indexPolicy, "indexPolicy must not be null");
    }

    /**
     * Returns the dataset ID that the setup SQL bundle targets.
     *
     * @return dataset ID that the setup SQL bundle targets
     */
    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    /**
     * Returns the setup SQL statements.
     *
     * @return setup SQL statements
     */
    public List<String> getSetupSqls() {
        return setupSqls;
    }

    /**
     * Returns the index handling policy.
     *
     * @return index handling policy
     */
    public IndexPolicy getIndexPolicy() {
        return indexPolicy;
    }
}
