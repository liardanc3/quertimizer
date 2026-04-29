package com.quertimizer.sqljudge.definition;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeReferenceId;

import java.util.Objects;

/**
 * Represents a registered reference SQL definition owned by sql-judge.
 */
public class ReferenceDefinition {

    private final JudgeReferenceId referenceId;
    private final JudgeDatasetId datasetId;
    private final String referenceSql;
    private final String resultHash;

    /**
     * Creates a registered reference SQL definition.
     *
     * @param referenceId reference SQL ID
     * @param datasetId dataset ID that the reference SQL targets
     * @param referenceSql reference SQL statement
     * @param resultHash canonical SQL result hash
     */
    public ReferenceDefinition(JudgeReferenceId referenceId,
                               JudgeDatasetId datasetId,
                               String referenceSql,
                               String resultHash) {
        this.referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.referenceSql = requireText(referenceSql, "referenceSql");
        this.resultHash = requireText(resultHash, "resultHash");
    }

    /**
     * Returns the reference SQL ID.
     *
     * @return reference SQL ID
     */
    public JudgeReferenceId getReferenceId() {
        return referenceId;
    }

    /**
     * Returns the dataset ID that the reference SQL targets.
     *
     * @return dataset ID that the reference SQL targets
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
     * Returns the canonical SQL result hash.
     *
     * @return canonical SQL result hash
     */
    public String getResultHash() {
        return resultHash;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value;
    }
}
