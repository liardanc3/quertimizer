package com.quertimizer.judge.domain.entity;

import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeReferenceId;

import java.util.Objects;

public class ReferenceDefinition {

    private final JudgeReferenceId referenceId;
    private final JudgeDatasetId datasetId;
    private final String referenceSql;
    private final String resultHash;

    public ReferenceDefinition(JudgeReferenceId referenceId,
                               JudgeDatasetId datasetId,
                               String referenceSql,
                               String resultHash) {
        this.referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
        this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
        this.referenceSql = requireText(referenceSql, "referenceSql");
        this.resultHash = requireText(resultHash, "resultHash");
    }

    public JudgeReferenceId getReferenceId() {
        return referenceId;
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public String getReferenceSql() {
        return referenceSql;
    }

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
