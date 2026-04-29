package com.quertimizer.sqljudge.definition;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeSetupSqlId;
import com.quertimizer.sqljudge.policy.IndexPolicy;

import java.util.List;
import java.util.Objects;

/**
 * Represents a registered setup SQL bundle owned by sql-judge.
 */
public class SetupSqlDefinition {

    private final JudgeSetupSqlId setupSqlId;
    private final JudgeDatasetId datasetId;
    private final List<String> setupSqls;
    private final IndexPolicy indexPolicy;

    /**
     * Creates a registered setup SQL bundle.
     *
     * @param setupSqlId setup SQL bundle ID
     * @param datasetId dataset ID that the setup SQL bundle targets
     * @param setupSqls setup SQL statements
     * @param indexPolicy index handling policy
     */
    public SetupSqlDefinition(JudgeSetupSqlId setupSqlId,
                              JudgeDatasetId datasetId,
                              List<String> setupSqls,
                              IndexPolicy indexPolicy) {
        this.setupSqlId = Objects.requireNonNull(setupSqlId, "setupSqlId must not be null");
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.setupSqls = List.copyOf(Objects.requireNonNull(setupSqls, "setupSqls must not be null"));
        this.indexPolicy = Objects.requireNonNull(indexPolicy, "indexPolicy must not be null");
    }

    /**
     * Returns the setup SQL bundle ID.
     *
     * @return setup SQL bundle ID
     */
    public JudgeSetupSqlId getSetupSqlId() {
        return setupSqlId;
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
