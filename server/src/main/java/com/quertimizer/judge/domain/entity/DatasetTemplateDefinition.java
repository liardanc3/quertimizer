package com.quertimizer.judge.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;

import java.time.Instant;
import java.util.Objects;

public class DatasetTemplateDefinition {

    private final JudgeDatasetId datasetId;
    private final DbmsType dbmsType;
    private final String templateVersion;
    private final String environmentName;
    private final Instant createdAt;

    public DatasetTemplateDefinition(JudgeDatasetId datasetId, DbmsType dbmsType,
                                     String templateVersion, String environmentName,
                                     Instant createdAt) {
        this.datasetId = Objects.requireNonNull(datasetId, "필수 값이 없다.");
        this.dbmsType = Objects.requireNonNull(dbmsType, "필수 값이 없다.");
        this.templateVersion = requireText(templateVersion, "templateVersion");
        this.environmentName = requireText(environmentName, "environmentName");
        this.createdAt = Objects.requireNonNull(createdAt, "필수 값이 없다.");
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public DbmsType getDbmsType() {
        return dbmsType;
    }

    public String getTemplateVersion() {
        return templateVersion;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있다.");
        }

        return value.trim();
    }
}
