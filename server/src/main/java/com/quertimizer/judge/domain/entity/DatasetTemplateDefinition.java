package com.quertimizer.judge.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

import java.time.Instant;

@Data
public class DatasetTemplateDefinition {

    private final JudgeDatasetId datasetId;
    private final DbmsType dbmsType;
    private final String templateVersion;
    private final String environmentName;
    private final Instant createdAt;

    public DatasetTemplateDefinition(JudgeDatasetId datasetId, DbmsType dbmsType,
                                     String templateVersion, String environmentName,
                                     Instant createdAt) {
        this.datasetId = datasetId;
        this.dbmsType = dbmsType;
        this.templateVersion = templateVersion.trim();
        this.environmentName = environmentName.trim();
        this.createdAt = createdAt;
    }
}
