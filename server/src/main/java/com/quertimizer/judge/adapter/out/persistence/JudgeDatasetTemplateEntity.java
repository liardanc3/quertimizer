package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.model.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "judge_dataset_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgeDatasetTemplateEntity {

    @Id
    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", insertable = false, updatable = false)
    private JudgeDatasetDefinitionEntity datasetDefinition;

    @Column(name = "dbms_type", nullable = false, length = 20)
    private String dbmsType;

    @Column(name = "template_version", nullable = false, length = 80)
    private String templateVersion;

    @Column(name = "environment_name", nullable = false, length = 80)
    private String environmentName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static JudgeDatasetTemplateEntity from(DatasetTemplateDefinition definition) {
        return new JudgeDatasetTemplateEntity(
                definition.getDatasetId().getValue(), definition.getDbmsType().name(),
                definition.getTemplateVersion(), definition.getEnvironmentName(), definition.getCreatedAt()
        );
    }

    public DatasetTemplateDefinition toDefinition() {
        return new DatasetTemplateDefinition(
                new JudgeDatasetId(datasetId), DbmsType.valueOf(dbmsType),
                templateVersion, environmentName, createdAt
        );
    }

    private JudgeDatasetTemplateEntity(Long datasetId, String dbmsType,
                                       String templateVersion, String environmentName, Instant createdAt) {
        this.datasetId = datasetId;
        this.dbmsType = dbmsType;
        this.templateVersion = templateVersion;
        this.environmentName = environmentName;
        this.createdAt = createdAt;
    }
}
