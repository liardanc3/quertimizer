package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "judge_dataset_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgeDatasetDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dataset_id", nullable = false, updatable = false)
    private Long datasetId;

    @Column(name = "base_index_ddls_json", nullable = false, columnDefinition = "TEXT")
    private String baseIndexDdlsJson;

    @OneToOne(mappedBy = "datasetDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private JudgeInlineDatasetDefinitionEntity inlineDefinition;

    @OneToOne(mappedBy = "datasetDefinition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private JudgeDatasetTemplateEntity templateDefinition;

    @OneToMany(mappedBy = "datasetDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JudgeSetupSqlDefinitionEntity> setupSqlDefinitions = new ArrayList<>();

    public static JudgeDatasetDefinitionEntity from(DatasetDefinition definition, String baseIndexDdlsJson,
                                                    boolean storeSqlDefinition) {
        JudgeDatasetDefinitionEntity entity = new JudgeDatasetDefinitionEntity(
                definition.getDatasetId() != null ? definition.getDatasetId().getValue() : null, baseIndexDdlsJson
        );
        if (storeSqlDefinition) {
            entity.inlineDefinition = JudgeInlineDatasetDefinitionEntity.from(entity, definition);
        }

        return entity;
    }

    public JudgeDatasetId toDatasetId() {
        return new JudgeDatasetId(datasetId);
    }

    public String getBaseIndexDdlsJson() {
        return baseIndexDdlsJson;
    }

    private JudgeDatasetDefinitionEntity(Long datasetId, String baseIndexDdlsJson) {
        this.datasetId = datasetId;
        this.baseIndexDdlsJson = baseIndexDdlsJson;
    }
}
